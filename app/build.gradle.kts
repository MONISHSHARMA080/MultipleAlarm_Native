import org.gradle.kotlin.dsl.testImplementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

android {
    namespace = "com.example.trying_native"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.trying_native"
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
            storeFile = file(System.getenv("ANDROID_KEYSTORE_FILE") ?: 
                project.findProperty("android.injected.signing.store.file")?.toString() ?: 
                "release.keystore")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?:
                project.findProperty("android.injected.signing.store.password")?.toString()
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?:
                project.findProperty("android.injected.signing.key.alias")?.toString()
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?:
                project.findProperty("android.injected.signing.key.password")?.toString()
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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

dependencies {
//    implementation("com.posthog:posthog-android:3.+")
    implementation(libs.androidx.espresso.contrib)
    implementation(libs.androidx.ui.test.junit4.android)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(libs.androidx.junit.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")


    // -- roboelectric tests ---
    testImplementation("junit:junit")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.4.5")
//    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")




    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.ui.test.android)
    val room_version = "2.6.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
//    kapt("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("androidx.room:room-guava:$room_version")
    testImplementation("androidx.room:room-testing:$room_version")
    implementation("androidx.room:room-paging:$room_version")

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.v100)
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