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
package cloud.dmytrominochkin.gradle.llamacpp

import cloud.dmytrominochkin.gradle.AndroidGpu
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject

abstract class LlamaCppExtension(
    objects: ObjectFactory
) {
    abstract val llamaCppDir: DirectoryProperty

    val ios: IosExtension = objects.newInstance(IosExtension::class.java, objects)
    val desktop: DesktopConfig = objects.newInstance(DesktopConfig::class.java)
    val android: AndroidConfig = objects.newInstance(AndroidConfig::class.java)

    fun ios(action: Action<in IosExtension>) = action.execute(ios)
    fun desktop(action: Action<in DesktopConfig>) = action.execute(desktop)
    fun android(action: Action<in AndroidConfig>) = action.execute(android)
}

abstract class IosExtension @Inject constructor(
    objects: ObjectFactory
) {
    val simulator: IosConfig = objects.newInstance(IosConfig::class.java)
    val device: IosConfig = objects.newInstance(IosConfig::class.java)

    fun simulator(action: Action<in IosConfig>) = action.execute(simulator)
    fun device(action: Action<in IosConfig>) = action.execute(device)
}

abstract class IosConfig {
    abstract val enabled: Property<Boolean>
    abstract val buildDir: DirectoryProperty
    abstract val sysroot: Property<String>
    abstract val architectures: Property<String>
    abstract val deploymentTarget: Property<String>
}

abstract class DesktopConfig {
    abstract val enabled: Property<Boolean>
    abstract val buildDir: DirectoryProperty
    abstract val enableMetal: Property<Boolean>
    abstract val enableCuda: Property<Boolean>
    abstract val enableVulkan: Property<Boolean>
    abstract val enableOpenMP: Property<Boolean>
    abstract val enableBlas: Property<Boolean>
    abstract val architectures: Property<String>
    abstract val macOsSharedLibs: Property<Boolean>
}

abstract class AndroidConfig {
    abstract val enabled: Property<Boolean>
    abstract val buildDir: DirectoryProperty
    abstract val ndkDirectory: Property<File>
    abstract val androidPlatform: Property<String>
    abstract val abi: Property<String>
    abstract val hostArch: Property<String>
    abstract val gpuType: Property<AndroidGpu>
}

