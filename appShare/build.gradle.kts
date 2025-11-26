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

    val frameworkName = "appShareKit"

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = frameworkName
            isStatic = true
            // export(project(":net"))
        }
    }

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
        iosMain {
            dependencies {
                // iOS runtime dependencies can be added here
            }
        }
    }

}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.tans.tfiletransporter.resources"
    generateResClass = auto
}
