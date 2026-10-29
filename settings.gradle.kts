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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Kadb 的 SPAKE2 配对实现发布在 JitPack，仅允许解析这一项依赖。
        maven("https://jitpack.io") {
            content { includeModule("com.github.Flyfish233", "spake2-java") }
        }
    }
}

rootProject.name = "test live update"
include(":composeApp")
include(":androidApp")
