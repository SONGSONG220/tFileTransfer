plugins {
    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    // Moshi
    api(libs.moshi)
    api(libs.moshi.kotlin)
    api(libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)

    api(libs.netty)
    api(libs.okio)
    api(libs.androidx.annotaion)
    api(libs.tlrucache)
}