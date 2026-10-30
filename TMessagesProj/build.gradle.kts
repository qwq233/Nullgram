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
import com.android.build.api.artifact.SingleArtifact
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.attributes.Usage
import java.text.SimpleDateFormat
import java.util.Date
import org.telegram.tasks.GenerateLottieMetadataAssetFileTask
import org.telegram.tasks.GenerateStringResourceIdsAssetTask
import org.telegram.tasks.TelegramStringsTask
import org.telegram.tasks.localization.GenerateLocalizationUtilsJavaTask
import org.telegram.transforms.CoreFingerprintTransform

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
val isCi = System.getenv("GITHUB_ACTIONS") == "true"

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

// Core 1.18+ dropped the backend used by stable Biometric on older Android versions.
check(libs.versions.coreKtx.get() == "1.19.1") {
    "Re-audit CoreFingerprintTransform before updating Core"
}
val coreStockAarType = "nullgram-core-stock-aar"
val coreFingerprintDonor = configurations.create("coreFingerprintDonor") {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
}

configurations.matching {
    it.name.endsWith("CompileClasspath") || it.name.endsWith("RuntimeClasspath")
}.configureEach {
    resolutionStrategy.dependencySubstitution.all {
        val request = requested as? ModuleComponentSelector
        if (request?.group == "androidx.core" && request.module == "core") {
            // Explicit AAR requests from older POMs would bypass the variant transform.
            artifactSelection {
                check(requestedSelectors.all {
                    it.type == "aar" && it.extension == "aar" && it.classifier.isNullOrEmpty()
                }) { "Unexpected Core artifact selector; re-audit fingerprint compatibility" }
                withoutArtifactSelectors()
            }
        }
    }
}

dependencies {
    components.withModule("androidx.core:core") {
        if (id.version == libs.versions.coreKtx.get()) {
            for (variant in listOf("releaseVariantReleaseApiPublication", "releaseVariantReleaseRuntimePublication")) {
                withVariant(variant) {
                    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, coreStockAarType)
                }
            }
        }
    }
    add(coreFingerprintDonor.name, variantOf(libs.androidx.core.fingerprint.compat) { artifactType("aar") })
    registerTransform(CoreFingerprintTransform::class) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, coreStockAarType)
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
        parameters.legacyCoreAar.set(
            layout.file(coreFingerprintDonor.elements.map { it.single().asFile })
        )
    }
    constraints {
        implementation("androidx.core:core") {
            version { strictly(libs.versions.coreKtx.get()) }
            because("The native fingerprint compatibility transform is audited for this Core version")
        }
    }
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.interpolator)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.sharetarget)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.projected)
    implementation(libs.zxing.core)
    implementation(libs.nv.websocket)
    implementation(libs.play.services.wearable)

    compileOnly(libs.checker.compat.qual)
    compileOnly(libs.checker.qual)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appindexing)
    implementation(libs.play.services.vision)
    implementation(libs.play.services.location)
    implementation(libs.play.services.wallet)
    implementation(libs.play.services.mlkit.vision)
    implementation(libs.play.services.mlkit.imageLabeling)
    implementation(libs.play.services.cast.framework)
    implementation(libs.isoparser)
    implementation(libs.stripe.payments.core)
    implementation(libs.language.id)
    implementation(files("libs/libgsaverification-client.aar"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    implementation(libs.process.phoenix)
    implementation(libs.hiddenapibypass)
    implementation(libs.nanohttpd)
    implementation(libs.recaptcha)
    implementation(libs.markwon.core)
    implementation(libs.markwon.strikethrough)
    implementation(libs.markwon.tables)
    implementation(libs.markwon.html)
    implementation(libs.markwon.inline.parser)
    implementation(libs.markwon.latex) {
        exclude(group = "ru.noties", module = "jlatexmath-android")
    }

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.dash)
    implementation(libs.media3.hls)
    implementation(libs.media3.session)
    implementation(project(":jlatexmath"))

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

            the<CrashlyticsExtension>().nativeSymbolUploadEnabled = isCi
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = isCi
            }
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
            isDefault = true
            isDebuggable = true
            isJniDebuggable = true
        }

        create("play") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
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
        buildConfigField("String", "BUILD_VERSION_STRING", "\"${Version.officialVersionName}\"")
        buildConfigField("String", "BETA_URL", "\"\"")
        buildConfigField("boolean", "DEBUG_VERSION", "true")
        buildConfigField("boolean", "DEBUG_PRIVATE_VERSION", "false")
        buildConfigField("boolean", "BUNDLE", isBuildingBundle.toString())
        buildConfigField("int", "VERSION_NUM", "7")
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
        val suffix = variant.name.replaceFirstChar { it.uppercase() }
        val localizations = tasks.register<GenerateLocalizationUtilsJavaTask>("generate${suffix}LocalizationUtilsJava") {
            localizationFiles.from(fileTree("src/main/res") { include("values-*/strings*.xml") })
            javaOutputDir.set(layout.buildDirectory.dir("generated/generateLocalizationUtilsJava/${variant.name}"))
        }
        variant.sources.java?.addGeneratedSourceDirectory(localizations, GenerateLocalizationUtilsJavaTask::javaOutputDir)

        val strings = tasks.register<TelegramStringsTask>("generate${suffix}TelegramStrings") {
            stringsXml.from(fileTree("src/main/res/values") { include("strings*.xml") })
            localizationFiles.from(fileTree("src/main/res") { include("values-*/strings*.xml") })
            stringsOutputDir.set(layout.buildDirectory.dir("generated/telegramStrings/${variant.name}/res"))
            assetsOutputDir.set(layout.buildDirectory.dir("generated/telegramStrings/${variant.name}/assets"))
            stableIdsFile.set(layout.buildDirectory.file("generated/telegramStrings/${variant.name}/stable-ids.txt"))
            resourcePackageName.set(variant.applicationId)
        }
        variant.sources.res?.addGeneratedSourceDirectory(strings, TelegramStringsTask::stringsOutputDir)
        variant.sources.assets?.addGeneratedSourceDirectory(strings, TelegramStringsTask::assetsOutputDir)
        variant.androidResources.aaptAdditionalParameters.addAll(strings.flatMap { task ->
            task.stableIdsFile.map { listOf("--stable-ids", it.asFile.absolutePath) }
        })

        val stringIds = tasks.register<GenerateStringResourceIdsAssetTask>("generate${suffix}StringResourceIdsAsset") {
            runtimeSymbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
            outputDir.set(layout.buildDirectory.dir("generated/stringResourceIds/${variant.name}/assets"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(stringIds, GenerateStringResourceIdsAssetTask::outputDir)
        val lottie = tasks.register<GenerateLottieMetadataAssetFileTask>("generate${suffix}LottieMeta") {
            runtimeSymbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
            rawResourceDirs.from(fileTree("src/main/res") { include("raw*/*.json") })
            outputDir.set(layout.buildDirectory.dir("generated/lottieMeta/${variant.name}/assets"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(lottie, GenerateLottieMetadataAssetFileTask::outputDir)

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
}
