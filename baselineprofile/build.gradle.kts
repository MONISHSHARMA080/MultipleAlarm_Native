
plugins {
	alias(libs.plugins.android.test)
	alias(libs.plugins.baselineprofile)
	alias(libs.plugins.kotlin.android)
}

android {
	namespace = "com.example.baselineprofile"
	compileSdk = 36

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}

	defaultConfig {
		minSdk = 33
		targetSdk = 36

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	targetProjectPath = ":app"

	testOptions {
		managedDevices {
			localDevices {
				create("pixel8") {
					device = "Pixel 8a"
					apiLevel = 34
					systemImageSource = "google"
				}
			}
		}
	}
}

kotlin {
	compilerOptions {
//		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
	}
}

// Baseline Profile plugin configuration
baselineProfile {
	// Use Gradle Managed Device for CI (better than useConnectedDevices)
	managedDevices += "pixel8"

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