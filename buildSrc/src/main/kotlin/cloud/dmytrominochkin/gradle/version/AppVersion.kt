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
package cloud.dmytrominochkin.gradle.version
val VERSION_PATTERN = Regex("([0-9]+)\\.([0-9]+)\\.([0-9]+)")

/**
 * The version of a release which is in the form of <MAJOR>.<FEATURE>.<PATCH>
 */
data class AppVersion(val major: Int, val feature: Int, val patch: Int) : Comparable<AppVersion> {

    companion object {
        fun fromIdent(ident: String): AppVersion {
            val m =
                VERSION_PATTERN.matchEntire(ident)
                    ?: throw RuntimeException("Not a valid release version: $ident")
            return AppVersion(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt()
            )
        }
    }

    /**
     * Generates a numeric version code from the (version) string.
     * Returns a monotonic increasing number, that fulfills the requirements of the version code on both
     * platforms.
     */
    fun getNumericAppVersion(): Int {
        return major * 1_000_000 + feature * 1_000 + patch
    }

    override fun compareTo(other: AppVersion): Int {

        return major.compareTo(other.major).let { majorDiff ->
            if (majorDiff != 0) majorDiff else feature.compareTo(other.feature).let { featureDiff ->
                if (featureDiff != 0) featureDiff else patch.compareTo(other.patch)
            }
        }
    }

    override fun toString(): String {
        return "$major.$feature.$patch"
    }
}
