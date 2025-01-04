import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("com.google.protobuf") version "0.9.4"
}

android {
    namespace = "com.example.trying_native"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.trying_native"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose =true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6" // Update to match Kotlin version
    }

     buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // If you're using compose, update this as well
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

// Protobuf configuration
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.23.4"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Force specific versions to resolve conflicts
        force("com.google.protobuf:protobuf-javalite:3.23.4")

        // Exclude protobuf-lite from all configurations
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
}

dependencies {
    // Core Proto dependencies
    implementation("androidx.datastore:datastore-core:1.1.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    implementation("androidx.datastore:datastore:1.1.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    implementation("com.google.protobuf:protobuf-javalite:3.23.4")

    // Other dependencies remain the same...
    implementation("com.posthog:posthog-android:3.+")
    implementation(libs.androidx.espresso.contrib)

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
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
        implementation(libs.androidx.datastore.v100)

//        implementation(libs.androidx.datastore.rxjava3)
//        implementation(libs.androidx.datastore)
//        implementation(libs.androidx.datastore.preferences)

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
