pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "OfflinePlayer"

include(":app")
include(":benchmark")

// Core modules
include(":core:model")
include(":core:database")
include(":core:library")
include(":core:playback")
include(":core:provider-api")
include(":core:download")

// Provider modules
include(":provider:local")

// Worker module
include(":worker")

// Feature modules
include(":feature:home")
include(":feature:search")
include(":feature:library")
include(":feature:player")
include(":feature:downloads")
include(":feature:settings")
