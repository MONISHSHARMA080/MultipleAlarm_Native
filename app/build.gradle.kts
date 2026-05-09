
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.devtools.ksp") version "2.3.5"
    id ("kotlin-parcelize")
    id("com.posthog.android") version "1.2.0"
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
	id("com.google.protobuf") version "0.10.0"
    id("com.google.dagger.hilt.android")

}

val myAppName="Multiple alarms"
configureAndroid()
fun Project.configureAndroid() {

    extensions.configure<ApplicationExtension> {

        namespace = "com.coolApps.MultipleAlarmClock"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.coolApps.MultipleAlarmClock"
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
            }
        }

        buildTypes {
            release {
                val isProductionBuild = file("release.keystore").exists()
                val appName = if (isProductionBuild) {
                    myAppName
                } else {
                    "debug-$myAppName"
                }
                val skipPostHog = project.findProperty("skipPostHog")?.toString() == "true"
                buildConfigField("boolean", "SKIP_POSTHOG", skipPostHog.toString())
                resValue("string", "app_name", appName)
                isShrinkResources = true
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                signingConfig = signingConfigs.getByName("release")
            }
            debug {
                applicationIdSuffix = ".debug"
                resValue("string", "app_name", "debug-$myAppName")
                buildConfigField("boolean", "SKIP_POSTHOG", "false")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

        buildFeatures {
            compose = true
            buildConfig = true
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
                    it.jvmArgs("-noverify")
                }

            }
        }
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:4.34.1"
	}
	generateProtoTasks {
		all().forEach { task ->
			task.builtins {
				create("java") {
					option("lite")
				}
				create("kotlin")
			}
		}
	}
}



dependencies {
	configurations.configureEach {
		exclude(group = "com.google.protobuf", module = "protobuf-lite")
	}
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-android-compiler:2.59.2")

    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    implementation ("com.posthog:posthog-android:3.44.1")
    implementation("androidx.core:core-splashscreen:1.2.0")

    implementation("com.google.android.gms:play-services-appset:16.1.0")

    implementation(libs.androidx.datastore)
	implementation(libs.protobuf.kotlin.lite.v4321)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation(libs.androidx.espresso.contrib)
    implementation(libs.androidx.ui.test.junit4.android)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation.layout)
	implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
	implementation(libs.androidx.ui.text)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.1")
    "baselineProfile"(project(":baselineprofile"))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(libs.androidx.junit.ktx)
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.work:work-runtime-ktx:2.11.1")

    // -- roboelectric tests ---
    testImplementation("junit:junit")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.4.5")

    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.ui.test.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
//    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
    implementation("androidx.room:room-rxjava3:$roomVersion")
    implementation("androidx.room:room-guava:$roomVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.uiautomator.v240alpha05)
}
