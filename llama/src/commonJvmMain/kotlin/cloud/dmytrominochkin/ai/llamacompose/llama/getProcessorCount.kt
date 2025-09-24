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
package cloud.dmytrominochkin.ai.llamacompose.llama

/**
 * Get the processor number. Considering most systems are using hyper threading,
 * thus the result will divide by 2.
 * */
actual fun getProcessorCount() = Runtime.getRuntime().availableProcessors().let {
    // if we have less than 8 core, then use it - 1 thread, leaving the last one to system
    // otherwise we take hyper threading into account and use half of them
    if (it <= 8) (it - 1).coerceAtLeast(1) else it / 2
}