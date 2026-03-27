pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Composite build: substituting the Sankofa SDK dynamically so this 
// example app uses the local source code instead of a published artifact.
includeBuild("../../sdks/sankofa_sdk_android") {
    dependencySubstitution {
        substitute(module("dev.sankofa.sdk:sankofa")).using(project(":sankofa"))
    }
}

include(":app")
rootProject.name = "sankofa-example-android"
