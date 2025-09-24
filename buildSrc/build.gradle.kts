plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("projectVersion") {
            id = "cloud.dmytrominochkin.plugins.project-version"
            implementationClass = "cloud.dmytrominochkin.gradle.version.ProjectVersionPlugin"
        }
        create("llamaCpp") {
            id = "cloud.dmytrominochkin.plugins.llamacpp"
            implementationClass = "cloud.dmytrominochkin.gradle.llamacpp.LlamaCppPlugin"
        }
    }
}
