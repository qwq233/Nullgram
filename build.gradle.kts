@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

val apiCode by extra(93)
val verCode = Common.getBuildVersionCode(rootProject)

val verName = if (Version.isStable) {
    "v" + Version.officialVersionName + "-" + (Common.getGitHeadRefsSuffix(rootProject))
} else {
    "v" + Version.officialVersionName + "-preview-" + (Common.getGitHeadRefsSuffix(rootProject))
}

val androidTargetSdkVersion by extra(33)
val androidMinSdkVersion by extra(24)
val androidCompileSdkVersion by extra(33)
val androidBuildToolsVersion by extra("33.0.2")
val androidCompileNdkVersion = "23.2.8568313"

fun Project.configureBaseExtension() {
    extensions.findByType(com.android.build.gradle.BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName
        }

        compileOptions {
            sourceCompatibility = Version.java
            targetCompatibility = Version.java
        }

        packagingOptions.jniLibs.useLegacyPackaging = false
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}
