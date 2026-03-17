
plugins {
	alias(libs.plugins.android.test)
	alias(libs.plugins.baselineprofile)
	alias(libs.plugins.kotlin.android)
}

android {
	namespace = "com.example.baselineprofile"
	compileSdk = 36

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	defaultConfig {
		minSdk = 33
		targetSdk = 36

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	targetProjectPath = ":app"

	// Configure Gradle Managed Device for CI
	testOptions {
		managedDevices {
			localDevices {  // <- This is the correct one!
				create("pixel6Api31") {
					device = "Pixel 8a"
					apiLevel = 33
					systemImageSource = "google"
				}
			}
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
	}
}

// Baseline Profile plugin configuration
baselineProfile {
	// Use Gradle Managed Device for CI (better than useConnectedDevices)
	managedDevices += "pixel6Api31"

	// Set to false for CI - we're using Gradle Managed Devices
	useConnectedDevices = false
}

dependencies {
	implementation(libs.androidx.junit)
	implementation(libs.androidx.espresso.core)
	implementation(libs.androidx.uiautomator)
	implementation(libs.androidx.benchmark.macro.junit4)
	implementation(libs.core.ktx)
}

androidComponents {
	onVariants { v ->
		val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
		v.instrumentationRunnerArguments.put(
			"targetAppId",
			v.testedApks.map { artifactsLoader.load(it)?.applicationId }
		)
	}
}


// --- adding the gradle managed device and asking claude to do vibe coding

//plugins {
//	alias(libs.plugins.android.test)
//	alias(libs.plugins.baselineprofile)
//	alias(libs.plugins.kotlin.android)
//}
//
//android {
//	namespace = "com.example.baselineprofile"
//	compileSdk {
//		version = release(36) {
//			minorApiLevel = 1
//		}
//	}
//
//	compileOptions {
//		sourceCompatibility = JavaVersion.VERSION_25
//		targetCompatibility = JavaVersion.VERSION_25
//	}
//
//	defaultConfig {
//		minSdk = 28
//		targetSdk = 36
//
//		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//	}
//
//	targetProjectPath = ":app"
//
//}
//
//kotlin {
//	compilerOptions {
//		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
//	}
//}
//
//// This is the configuration block for the Baseline Profile plugin.
//// You can specify to run the generators on a managed devices or connected devices.
//baselineProfile {
//}
//
//dependencies {
//	implementation(libs.androidx.junit)
//	implementation(libs.androidx.espresso.core)
//	implementation(libs.androidx.uiautomator)
//	implementation(libs.androidx.benchmark.macro.junit4)
//	implementation(libs.core.ktx)
//
//
//}
//
//androidComponents {
//	onVariants { v ->
//		val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
//		v.instrumentationRunnerArguments.put(
//			"targetAppId",
//			v.testedApks.map { artifactsLoader.load(it)?.applicationId }
//		)
//	}
//}