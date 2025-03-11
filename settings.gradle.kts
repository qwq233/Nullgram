/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

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
    id("com.gradle.develocity") version "3.19.2"
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
