@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
}

configurations {
    all {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "androidx.recyclerview", module = "recyclerview")
    }
}

var serviceAccountCredentialsFile = File(rootProject.projectDir, "service_account_credentials.json")
val abiName = mapOf("armeabi-v7a" to "arm32", "arm64-v8a" to "arm64", "x86" to "x86", "x86_64" to "x86_64")

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

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ndk)

    implementation(libs.appcenter.analytics)
    implementation(libs.appcenter.crashes)

    implementation(libs.core.ktx)
    implementation(libs.palette.ktx)
    implementation(libs.exifinterface)
    implementation(libs.dynamicanimation)
    implementation(libs.interpolator)
    implementation(libs.sharetarget)

    compileOnly(libs.checker.compat.qual)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.datatransport)
    implementation(libs.firebase.appindexing)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.vision)
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.location)
    implementation(libs.play.services.wallet)
//    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
    implementation(libs.isoparser)
    implementation(files("libs/stripe.aar"))
    implementation(libs.language.id)
    implementation(files("libs/libgsaverification-client.aar"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    implementation(libs.process.phoenix)
    implementation(libs.licensesdialog)
    implementation(libs.markwon.core)
    implementation(libs.hiddenapibypass)
    implementation(libs.prism4j)

    implementation(libs.kotlin.stdlib.common)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.osmdroid.android)
    implementation(libs.billing)
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
        java.srcDir("src/main/java")
        jniLibs.srcDirs("./jni/")
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

    kotlin {
        jvmToolchain(Version.java.toString().toInt())
    }

    var keystorePwd: String? = null
    var alias: String? = null
    var pwd: String? = null
    if (project.rootProject.file("local.properties").exists()) {
        keystorePwd = gradleLocalProperties(rootDir).getProperty("RELEASE_STORE_PASSWORD")
        alias = gradleLocalProperties(rootDir).getProperty("RELEASE_KEY_ALIAS")
        pwd = gradleLocalProperties(rootDir).getProperty("RELEASE_KEY_PASSWORD")
    }

    signingConfigs {
        create("release") {
            storeFile = File(projectDir, "config/release.keystore")
            storePassword = (keystorePwd ?: System.getenv("KEYSTORE_PASS"))
            keyAlias = (alias ?: System.getenv("ALIAS_NAME"))
            keyPassword = (pwd ?: System.getenv("ALIAS_PASS"))
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
            optimization {
                keepRules {
                    ignoreExternalDependencies("com.microsoft.appcenter:appcenter")
                }
            }

            the<CrashlyticsExtension>().nativeSymbolUploadEnabled = true
            the<CrashlyticsExtension>().mappingFileUploadEnabled = true
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

    defaultConfig {
        externalNativeBuild {
            cmake {
                version = "3.22.1"
                arguments += listOf(
                    "-DANDROID_STL=c++_static", "-DANDROID_PLATFORM=android-21", "-DCMAKE_C_COMPILER_LAUNCHER=ccache", "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache", "-DNDK_CCACHE=ccache"
                )
            }
        }

        buildConfigField("String", "BUILD_TIME", "\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    androidComponents {
        onVariants { variant ->
            variant.buildConfigFields.put("isPlay", BuildConfigField("boolean", variant.name == "play", null))
            variant.outputs.forEach { output ->
                val abi = output.filters.find { it.filterType == ABI }?.identifier
                variant.buildConfigFields.put(
                    "FLAVOR", BuildConfigField(
                        "String", "\"${abiName[abi]}\"",
                        "this is just a compatibility solution and we are not using flavorProduct anymore"
                    )
                )
            }
        }
    }

    applicationVariants.all {
        outputs.all {
            val abi = this.filters.find { it.filterType == com.android.build.VariantOutput.ABI }?.identifier
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val outputFileName = "Nullgram-${defaultConfig.versionName}-${abiName[abi]}.apk"
            output?.outputFileName = outputFileName
        }
    }


}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
}
