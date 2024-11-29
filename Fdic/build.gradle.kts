plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.powerassert)
    alias(libs.plugins.android.library)
    id("module.publication")
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvm.get()
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = libs.versions.jvm.get()
            }
        }
    }
    wasmJs {
        moduleName = "fdic"
        browser {
            commonWebpackConfig {
                outputFileName = "fdicLibrary.js"
            }
        }
        binaries.library()
    }
//    wasmWasi {}
    js(IR)
    iosX64()
    iosArm64()
    iosSimulatorArm64()
//    linuxX64()
//    macosX64()
//    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.okio)
                implementation(libs.korge.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.darkrockstudios.fdic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
    }
}