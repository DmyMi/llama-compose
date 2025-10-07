/*
   Copyright 2025 Dmytro Minochkin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package cloud.dmytrominochkin.ai.llamacompose.download

import cloud.dmytrominochkin.ai.llamacompose.PlatformContext
import cloud.dmytrominochkin.ai.llamacompose.getUserDataDir
import cloud.dmytrominochkin.ai.llamacompose.resources.Res
import cloud.dmytrominochkin.ai.llamacompose.resources.model_queued
import cloud.dmytrominochkin.ai.llamacompose.runAsync
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class KtorModelRepository(
    private val context: PlatformContext
) : ModelRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = Channel<Cmd>(Channel.BUFFERED)

    private val _models = MutableStateFlow(produceInitialList())
    override val models: StateFlow<List<ModelUiState>> = _models

    private val client = getHttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response -> response.status.value in 500..599 }
            retryOnExceptionIf { _, cause -> cause is IOException }
            delayMillis { retry -> (500L * (retry + 1)) }
        }
    }

    private var activeFilename: String? = null
    private var currentJob: Job? = null
    private val queue = ArrayDeque<String>()
    private val pending = mutableSetOf<String>()

    init { scope.launch { eventLoop() } }

    override suspend fun refresh() {
        // Best-effort clean of stale temps
        val dir = ensureModelsDir()
        FileSystem.SYSTEM
            .list(dir)
            .map { f ->
                runAsync(Dispatchers.IO) {
                    if (f.name.endsWith(".downloading")) {
                        runCatching {
                            FileSystem.SYSTEM.delete(f, false)
                        }
                    }
                }
            }.awaitAll()
        _models.value = produceInitialList()
    }

    override suspend fun startDownload(model: LlmModel) {
        val message = getString(Res.string.model_queued)
        updateState(model) { it.copy(statusMessage = message, errorMessage = null) }
        commands.trySend(Cmd.Start(model.filename))
    }

    override suspend fun cancelDownload(model: LlmModel) {
        commands.trySend(Cmd.Cancel(model.filename))
    }

    override suspend fun deleteLocal(model: LlmModel) {
        commands.trySend(Cmd.Delete(model.filename))
    }

    override fun localPathFor(model: LlmModel): String? {
        val dir = ensureModelsDir()
        val finalFile = dir / model.filename
        return if (FileSystem.SYSTEM.exists(finalFile)) finalFile.absolute().toString() else null
    }

    private suspend fun eventLoop() {
        while (scope.isActive) {
            when (val cmd = commands.receive()) {
                is Cmd.Start -> {
                    val filename = cmd.filename
                    if (activeFilename == filename || pending.contains(filename)) continue
                    pending.add(filename)
                    queue.addLast(filename)
                    maybeStartNext()
                }
                is Cmd.Cancel -> {
                    if (activeFilename == cmd.filename) currentJob?.cancel() else {
                        if (pending.remove(cmd.filename)) queue.remove(cmd.filename)
                        findModel(cmd.filename)?.let { model -> updateState(model) { it.copy(status = ModelStatus.READY, statusMessage = null) } }
                    }
                }
                is Cmd.Delete -> {
                    if (activeFilename == cmd.filename) currentJob?.cancel()
                    pending.remove(cmd.filename)
                    queue.remove(cmd.filename)
                    val model = findModel(cmd.filename) ?: continue
                    val dir = ensureModelsDir()
                    val finalFile = dir / model.filename
                    val tempFile = dir / "${model.filename}.downloading"
                    FileSystem.SYSTEM.delete(finalFile, false)
                    FileSystem.SYSTEM.delete(tempFile, false)
                    updateState(model) { it.copy(status = ModelStatus.READY, progress = 0.0, hasPartial = false, progressIndeterminate = false, bytesPerSecond = null, etaSeconds = null, statusMessage = null, errorMessage = null) }
                }
            }
        }
    }

    private fun maybeStartNext() {
        if (activeFilename != null || queue.isEmpty()) return
        val next = queue.removeFirst()
        pending.remove(next)
        val model = findModel(next) ?: return
        activeFilename = next
        updateState(model) { it.copy(status = ModelStatus.DOWNLOADING, statusMessage = null, errorMessage = null) }
        currentJob = scope.launch {
            try {
                downloadOne(model)
                updateState(model) { it.copy(status = ModelStatus.DOWNLOADED, progress = 1.0, hasPartial = false, progressIndeterminate = false, bytesPerSecond = null, etaSeconds = null) }
            } catch (_: CancellationException) {
                updateState(model) { it.copy(status = ModelStatus.READY) }
            } catch (e: Exception) {
                updateState(model) { it.copy(status = ModelStatus.ERROR, errorMessage = e.message) }
            } finally {
                activeFilename = null
                currentJob = null
                maybeStartNext()
            }
        }
    }

    private suspend fun downloadOne(model: LlmModel) {
        val dir = ensureModelsDir()
        val finalFile = dir / model.filename
        val tempFile = dir / "${model.filename}.downloading"

        if (FileSystem.SYSTEM.exists(finalFile)) return
        FileSystem.SYSTEM.delete(tempFile, false)

        client.prepareGet(model.sourceUrl).execute { response ->
            val totalBytes = response.contentLength()

            if (totalBytes == null) updateState(model) { it.copy(progressIndeterminate = true) }
            else updateState(model) { it.copy(progress = 0.0, progressIndeterminate = false) }

            FileSystem.SYSTEM.write(finalFile, false) {
                val channel: ByteReadChannel = response.bodyAsChannel()
                val buffer = ByteArray(64 * 1024)
                var bytesCopied = 0L
                var lastEmitted = -1.0
                val timeSource = TimeSource.Monotonic
                var lastReportTime = timeSource.markNow()
                var lastReportBytes = 0L
                var lastIndeterminateReport = 0.milliseconds

                while (!channel.isClosedForRead && currentJob?.isActive == true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    if (read == 0) continue
                    write(buffer, 0, read)
                    bytesCopied += read

                    val now = timeSource.markNow()
                    if (totalBytes != null && totalBytes > 0L) {
                        val progress = (bytesCopied.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0)
                        val shouldEmit = lastEmitted < 0.0 || progress >= 0.999 || progress - lastEmitted >= 0.01
                        if (shouldEmit) {
                            val deltaBytes = (bytesCopied - lastReportBytes).coerceAtLeast(0L)
                            val deltaMs = (now - lastReportTime).inWholeMilliseconds.coerceAtLeast(1L)
                            val bps = (deltaBytes * 1000L) / deltaMs
                            val remaining = (totalBytes - bytesCopied).coerceAtLeast(0L)
                            val etaSec = if (bps > 0L) remaining / bps else null
                            updateState(model) { it.copy(progress = progress, progressIndeterminate = false, bytesPerSecond = bps, etaSeconds = etaSec) }
                            lastEmitted = progress
                            lastReportTime = now
                            lastReportBytes = bytesCopied
                        }
                    } else {
                        val elapsed = now.elapsedNow()
                        if (elapsed - lastIndeterminateReport >= 750.milliseconds) {
                            val deltaBytes = (bytesCopied - lastReportBytes).coerceAtLeast(0L)
                            val deltaMs = (now - lastReportTime).inWholeMilliseconds.coerceAtLeast(1L)
                            val bps = (deltaBytes * 1000L) / deltaMs
                            updateState(model) { it.copy(progressIndeterminate = true, bytesPerSecond = bps, etaSeconds = null) }
                            lastIndeterminateReport = elapsed
                            lastReportTime = now
                            lastReportBytes = bytesCopied
                        }
                    }
                }
            }
        }

        if (currentJob?.isActive == false) return

        if (FileSystem.SYSTEM.exists(tempFile)) {
            // TODO: remove delete due to atomic move?
            FileSystem.SYSTEM.delete(finalFile, false)
            FileSystem.SYSTEM.atomicMove(tempFile, finalFile)
            if (!FileSystem.SYSTEM.exists(finalFile)) throw IllegalStateException("Failed to finalize file")
        }
    }

    private fun ensureModelsDir(): Path {
        val base = getUserDataDir(context, "LlamaCompose") / "models"
        FileSystem.SYSTEM.createDirectory(base, false)
        return base
    }

    private fun produceInitialList(): List<ModelUiState> {
        val dir = ensureModelsDir()
        return DefaultModels.list.map { model ->
            val finalFile = dir / model.filename
            if (FileSystem.SYSTEM.exists(finalFile)) ModelUiState(model, status = ModelStatus.DOWNLOADED, progress = 1.0) else ModelUiState(model, status = ModelStatus.READY)
        }
    }

    private fun updateState(model: LlmModel, transform: (ModelUiState) -> ModelUiState) {
        _models.value = _models.value.map { if (it.model.filename == model.filename) transform(it) else it }
    }

    private sealed interface Cmd {
        data class Start(val filename: String) : Cmd
        data class Cancel(val filename: String) : Cmd
        data class Delete(val filename: String) : Cmd
    }

    private fun findModel(filename: String): LlmModel? = DefaultModels.list.find { it.filename == filename }
}
