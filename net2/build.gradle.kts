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

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
//    val xcfName = "netKit"
//
//    iosX64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    iosArm64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    iosSimulatorArm64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                api(libs.kotlinx.coroutines.core)
                api(libs.ktor.network)
                api(libs.okio)
                api(libs.kotlinx.atomicfu)
            }
        }

        androidMain {
            dependencies {
                api(libs.tlrucache)
                api(libs.tlog)
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