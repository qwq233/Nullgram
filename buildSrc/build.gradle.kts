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
        implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    }
}

java {
    targetCompatibility = java
    sourceCompatibility = java
}

kotlin {
    jvmToolchain(java.toString().toInt())
}
