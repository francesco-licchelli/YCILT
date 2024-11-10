plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
	namespace = "com.example.ycilt"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.example.ycilt"
		minSdk = 33
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"

		vectorDrawables {
			useSupportLibrary = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
	buildFeatures {
		compose = true
		viewBinding = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.1"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)
	implementation(libs.play.services.maps)
	implementation(libs.maps.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.play.services.location)
	implementation(libs.androidx.cardview)
	implementation(libs.material)
	implementation(libs.gson)
	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.mobile.ffmpeg.full)
	implementation(libs.okhttp)
	implementation(libs.retrofit)
	implementation(libs.converter.gson)
	implementation(libs.androidx.work.runtime.ktx)
	implementation(libs.core.ktx)
}

