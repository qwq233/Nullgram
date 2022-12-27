plugins {
    java
    kotlin("jvm")
}

dependencies {
    implementation("com.neovisionaries:nv-websocket-client:2.14")

}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}
