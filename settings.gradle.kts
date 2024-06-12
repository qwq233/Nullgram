@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        maven("https://jitpack.io")
    }
}

plugins {
    id("com.gradle.develocity") version "3.17"
}

develocity {
    buildScan {
        publishing.onlyIf {
            System.getenv("GITHUB_ACTIONS") == "true" || it.buildResult.failures.isNotEmpty()
        }
        termsOfUseAgree.set("yes")
        termsOfUseUrl.set("https://gradle.com/terms-of-service")
    }
}

rootProject.name = "Nullgram"
include(
    ":TMessagesProj",
    ":libs:tcp2ws",
    ":libs:pangu",
    ":libs:ksp"
)
