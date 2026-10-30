plugins {
    java
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.nv.websocket)

}

java {
    targetCompatibility = Version.java
    sourceCompatibility = Version.java
}


kotlin {
    jvmToolchain(Version.java.toString().toInt())
}
