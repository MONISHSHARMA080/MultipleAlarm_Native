import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.testImplementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.devtools.ksp") version "2.3.5"
    id ("kotlin-parcelize")
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)

}
val myAppName="Multiple alarms"
configureAndroid()
fun Project.configureAndroid() {
    extensions.configure<ApplicationExtension> {
        namespace = "com.example.trying_native"
//        namespace = "com.example.MultipleAlarms"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.coolApps.trying_native"
//            applicationId = "com.coolApps.MultipleAlarms"
            minSdk = 33
            targetSdk = 36
            versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
            versionName = project.findProperty("versionName") as String? ?: "1.0.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables {
                useSupportLibrary = true
            }
        }

        signingConfigs {
            create("release") {
                val keystoreFile = file("release.keystore")
                if (keystoreFile.exists()) {
                    // Production signing (CI/CD)
                    storeFile = keystoreFile
                    storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                        ?: project.findProperty("android.injected.signing.store.password")?.toString()
                    keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                        ?: project.findProperty("android.injected.signing.key.alias")?.toString()
                    keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                        ?: project.findProperty("android.injected.signing.key.password")?.toString()
                } else {
                    // Local development - use debug keystore
                    storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }

//                storeFile = file(System.getenv("ANDROID_KEYSTORE_FILE") ?:
//                project.findProperty("android.injected.signing.store.file")?.toString() ?:
//                "release.keystore")
//                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?:
//                        project.findProperty("android.injected.signing.store.password")?.toString()
//                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?:
//                        project.findProperty("android.injected.signing.key.alias")?.toString()
//                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?:
//                        project.findProperty("android.injected.signing.key.password")?.toString()
            }
        }

        buildTypes {
            release {
                val isProductionBuild = file("release.keystore").exists()
                val appName = if (isProductionBuild) {
                    myAppName  // "Multiple Alarms" for production
                } else {
                    "debug-$myAppName"  // "debug-Multiple Alarms" for local
                }

                resValue("string", "app_name", appName)
                isShrinkResources = true
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                signingConfig = signingConfigs.getByName("release")
            }
            debug {
                applicationIdSuffix = ".debug"  // Makes it com.example.trying_native.debug
                resValue("string", "app_name", "debug-$myAppName")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
        }

        buildFeatures {
            compose = true
        }
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
        configurations.all {
            resolutionStrategy {
                force("androidx.test.espresso:espresso-core:3.6.1")
            }
        }
        testOptions {
            unitTests {
                isIncludeAndroidResources = true
                // Add these for Robolectric
                all {
                    it.systemProperty("robolectric.logging", "stdout")
                    it.systemProperty("robolectric.graphicsMode", "NATIVE")
                    // Increase memory for tests
                    // Important for avoiding bytecode verification errors
                    it.jvmArgs("-noverify")
                }

            }
        }
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

dependencies {
//    implementation("com.posthog:posthog-android:3.+")
    implementation(libs.androidx.espresso.contrib)
    implementation(libs.androidx.ui.test.junit4.android)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation.layout)
	implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(libs.androidx.junit.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.9.7")


    // -- roboelectric tests ---
    testImplementation("junit:junit")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.4.5")
//    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.ui.test.android)
    val roomVersion = "2.8.4"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
//    kapt("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
    implementation("androidx.room:room-rxjava3:$roomVersion")
    implementation("androidx.room:room-guava:$roomVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.rxjava3)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    // notification
    implementation("androidx.core:core-ktx:1.15.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}