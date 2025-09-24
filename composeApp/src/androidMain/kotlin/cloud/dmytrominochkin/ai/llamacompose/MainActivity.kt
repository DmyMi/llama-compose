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
package cloud.dmytrominochkin.ai.llamacompose

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cloud.dmytrominochkin.ai.llamacompose.di.KoinApp
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists

class LlamaComposeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Experimenting with getting OpenCL to load if not directly exposed
        // Unfortunately, didn't help to load 'hidden' OpenCL driver on my phone
        if (isSamsungDevice() && isOpenClPresent()) {
            setupOpenClIcdVendors()
        }
        System.setProperty("kotlin-logging-to-android-native", "true")
    }

    private fun setupOpenClIcdVendors() {
        val tag = "LlamaCompose"
        try {
            val originalDir = File("/etc/OpenCL/vendors")
            if (originalDir.exists()) return

            val vendorsDir = File(filesDir, "opencl/vendors")
            if (!vendorsDir.exists()) vendorsDir.mkdirs()

            val candidates = listOf(
                "/vendor/lib64/libOpenCL.so",
                "/vendor/lib64/libGLES_mali.so",
                "/vendor/lib64/egl/libGLES_mali.so",
                "/system/vendor/lib64/libOpenCL.so",
                "/system/vendor/lib64/libGLES_mali.so",
                "/system/vendor/lib64/egl/libGLES_mali.so"
            )

            val vendorLib = candidates.firstOrNull { path ->
                val f = File(path)
                f.exists() and f.canRead()
            }

            if (vendorLib != null) {
                val icdFile = File(vendorsDir, "mali.icd")
                icdFile.writeText("$vendorLib\n")
                android.system.Os.setenv("OCL_ICD_VENDORS", vendorsDir.absolutePath, true)
                Log.i(tag, "OpenCL ICD configured: ${icdFile.absolutePath} -> $vendorLib")
            } else {
                Log.w(tag, "No Mali OpenCL vendor library found; running CPU-only")
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to setup OpenCL ICD", t)
        }
    }

    private fun isOpenClPresent() = try {
        val fullLibraryFileName = System.mapLibraryName("ggml-opencl")
        Paths.get(applicationInfo.nativeLibraryDir, fullLibraryFileName).exists()
    } catch (_: Throwable) {
        false
    }

    private fun isSamsungDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER
        return manufacturer.contains("samsung")
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KoinApp {
                App()
            }
        }
    }
}
