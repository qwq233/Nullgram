@file:Suppress("UnstableApiUsage")
plugins {
    `kotlin-dsl`
}

val java = JavaVersion.VERSION_17

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()

    dependencies {
        implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")
    }
}

java {
    targetCompatibility = java
    sourceCompatibility = java
}

kotlin {
    jvmToolchain(java.toString().toInt())
}
