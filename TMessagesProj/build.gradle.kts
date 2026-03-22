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

import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.FilterConfiguration
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    //alias(libs.plugins.rust)
}

configurations {
    all {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "androidx.recyclerview", module = "recyclerview")
    }
}

var serviceAccountCredentialsFile = File(rootProject.projectDir, "service_account_credentials.json")
val abiName = mapOf("armeabi-v7a" to "arm32", "arm64-v8a" to "arm64")

if (serviceAccountCredentialsFile.isFile) {
    setupPlay(Version.isStable)
    play.serviceAccountCredentials.set(serviceAccountCredentialsFile)
} else if (System.getenv().containsKey("ANDROID_PUBLISHER_CREDENTIALS")) {
    setupPlay(Version.isStable)
}

fun setupPlay(stable: Boolean) {
    val targetTrace = if (stable) "production" else "beta"
    play {
        track.set(targetTrace)
        defaultToAppBundles.set(true)
    }
}

//cargo {
//    module  = "../libs/rust"
//    libname = "rust"
//    targets = listOf("arm64", "arm")
//
//    prebuiltToolchains = true
//    profile = "release"
//}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.interpolator)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.sharetarget)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.credentials)

    compileOnly(libs.checker.compat.qual)
    compileOnly(libs.checker.qual)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.play.services.vision)
    implementation(libs.play.services.location)
    implementation(libs.play.services.wallet)
    implementation(libs.play.services.mlkit.vision)
    implementation(libs.play.services.mlkit.imageLabeling)
    implementation(libs.play.services.cast.framework)
    implementation(libs.isoparser)
    implementation(files("libs/stripe.aar"))
    implementation(libs.language.id)
    implementation(files("libs/libgsaverification-client.aar"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    implementation(libs.process.phoenix)
    implementation(libs.hiddenapibypass)
    implementation(libs.nanohttpd)
    implementation(libs.recaptcha)

    implementation(libs.kotlin.stdlib.common)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.osmdroid.android)
    implementation(libs.guava)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    implementation(project(":libs:tcp2ws"))
    implementation(project(":libs:pangu"))
    ksp(project(":libs:ksp"))
}

android {
    defaultConfig.applicationId = "top.qwq2333.nullgram"
    namespace = "org.telegram.messenger"

    sourceSets.getByName("main") {
        java.directories.add("src/main/java")
        jniLibs.directories.add("./jni/")
    }

    externalNativeBuild {
        cmake {
            path = File(projectDir, "jni/CMakeLists.txt")
        }
    }

    lint {
        checkReleaseBuilds = true
        disable += listOf(
            "MissingTranslation", "ExtraTranslation", "BlockedPrivateApi"
        )
    }

    packaging {
        resources.excludes += "**"
    }

    signingConfigs {
        create("release") {
            storeFile = File(projectDir, "config/release.keystore")
            gradleLocalProperties(rootDir, providers).apply {
                storePassword = getProperty("RELEASE_STORE_PASSWORD", System.getenv("KEYSTORE_PASS"))
                keyAlias = getProperty("RELEASE_KEY_ALIAS", System.getenv("ALIAS_NAME"))
                keyPassword = getProperty("RELEASE_KEY_PASSWORD", System.getenv("ALIAS_PASS"))
            }

            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(File(projectDir, "proguard-rules.pro"))

            the<CrashlyticsExtension>().nativeSymbolUploadEnabled = true
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
            isDefault = true
            isDebuggable = true
            isJniDebuggable = true
        }

        create("play") {
            initWith(getByName("release"))
        }
    }

    buildFeatures {
        buildConfig = true
    }

    //noinspection WrongGradleMethod
    val isBuildingBundle = gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }

    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    "-DANDROID_PLATFORM=android-27",
                    "-DCMAKE_C_COMPILER_LAUNCHER=ccache",
                    "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache",
                    "-DNDK_CCACHE=ccache",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                )
            }
        }
        ndk {
            if (isBuildingBundle) {
                abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
            }
        }
        buildConfigField("String", "BUILD_TIME", "\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\"")
    }

    splits {
        abi {
            isEnable = !isBuildingBundle
            reset()
            include("armeabi-v7a", "arm64-v8a")
        }
    }
}

base {
    archivesName.set("Nullgram")
}

androidComponents {
    onVariants { variant ->
        variant.buildConfigFields!!.put("isPlay", BuildConfigField("boolean", variant.name.lowercase() == "play", null))

        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier
            val mappedAbi = when (abi) {
                "arm64-v8a" -> "arm64"
                "armeabi-v7a" -> "arm32"
                else -> abi ?: "universal"
            }
            val vName = android.defaultConfig.versionName
            val oName = "Nullgram-$vName-$mappedAbi.apk"
            
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(oName)
        }
    }
}

kotlin {
    jvmToolchain(Version.java.toString().toInt())
    sourceSets.configureEach {
        kotlin.srcDir("${layout.buildDirectory.asFile.get().absolutePath}/generated/ksp/$name/kotlin/")
    }
}
