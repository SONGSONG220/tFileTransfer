plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {


    androidLibrary {
        namespace = "com.tans.tfiletransporter"
        compileSdk = properties["ANDROID_COMPILE_SDK"].toString().toInt()
        minSdk = properties["ANDROID_MIN_SDK"].toString().toInt()
        androidResources.enable = true
//        withHostTestBuilder {
//        }
//
//        withDeviceTestBuilder {
//            sourceSetTreeName = "test"
//        }.configure {
//            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        }
    }

    jvm()

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
//    val xcfName = "commonKit"
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
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.jetbrains.lifecycle.compose)
                implementation(libs.jetbrains.navigation.compose)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.uiTooling)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.material3)
                implementation(compose.components.resources)
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.activity.compose)
                implementation(libs.kotlinx.coroutines.android)
                implementation(project(":net"))
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core.jvm)
                implementation(project(":net"))
            }
        }
//
//        iosMain {
//            dependencies {
//                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
//                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
//                // part of KMP’s default source set hierarchy. Note that this source set depends
//                // on common by default and will correctly pull the iOS artifacts of any
//                // KMP dependencies declared in commonMain.
//            }
//        }
    }

}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.tans.tfiletransporter.resources"
    generateResClass = auto
}