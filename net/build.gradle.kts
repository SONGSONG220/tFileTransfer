plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {

    androidLibrary {
        namespace = "com.tans.tfiletransfer.net"
        compileSdk = properties["ANDROID_COMPILE_SDK"].toString().toInt()
        minSdk = properties["ANDROID_MIN_SDK"].toString().toInt()
    }

    jvm()

    val xcfName = "netKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                api(libs.kotlinx.coroutines.core)
                api(libs.ktor.network)
                api(libs.okio)
                api(libs.kotlinx.atomicfu)
                api(libs.kotlinx.serialization.json)
            }
        }

        androidMain {
            dependencies {
                api(libs.tlrucache)
                api(libs.tlog)
            }
        }

        iosMain {
            dependencies {
                
            }
        }

        jvmMain {
            dependencies {
                api(libs.tlrucache)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit)
            }
        }
    }

}