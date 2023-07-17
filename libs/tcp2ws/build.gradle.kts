plugins {
    java
    kotlin("jvm")
}

dependencies {
    implementation("com.neovisionaries:nv-websocket-client:2.14")

}

java {
    targetCompatibility = Version.java
    sourceCompatibility = Version.java
}


kotlin {
    jvmToolchain(Version.java.toString().toInt())
}
