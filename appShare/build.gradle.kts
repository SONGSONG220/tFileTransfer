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
                api(libs.kotlin.stdlib)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.atomicfu)
                api(libs.jetbrains.lifecycle.viewmodel)
                api(libs.jetbrains.lifecycle.compose)
                api(libs.jetbrains.navigation.compose)

                api(compose.foundation)
                api(compose.runtime)
                api(compose.ui)
                api(compose.material3)
                api(compose.components.resources)
                api(project(":net"))

                // TODO: Preview not work, only support JetBrains Fleet, Not support Android Studio or IDEA.
//                api(compose.uiTooling)
//                api(compose.components.uiToolingPreview)
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.activity.compose)
                api(libs.kotlinx.coroutines.android)
            }
        }

        jvmMain {
            dependencies {
                api(libs.kotlinx.coroutines.core.jvm)
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