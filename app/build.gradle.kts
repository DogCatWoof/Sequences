import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

configurations.all {
    resolutionStrategy {
        force("io.grpc:grpc-core:1.68.0")
        force("io.grpc:grpc-android:1.68.0")
        force("io.grpc:grpc-okhttp:1.68.0")
        force("io.grpc:grpc-protobuf-lite:1.68.0")
        force("io.grpc:grpc-stub:1.68.0")
    }
}

val localProps = Properties()
val localPropsFile = rootProject.file("local-only/local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream -> localProps.load(stream) }
}

val googleServicesSource = rootProject.file("local-only/google-services.json")
val googleServicesTarget = layout.projectDirectory.file("google-services.json").asFile
if (googleServicesSource.exists() && (!googleServicesTarget.exists() || googleServicesSource.lastModified() > googleServicesTarget.lastModified())) {
    googleServicesSource.copyTo(googleServicesTarget, overwrite = true)
}

android {
    namespace = "org.meow.sequences"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.meow.autistic"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"${localProps.getProperty("firebase.web.client.id", "")}\"")
    }

    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/NOTICE")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.register("run") {
    dependsOn("installDebug")
    group = "install"
    description = "Builds, installs, and launches the debug APK on a connected device"
    doLast {
        val process = ProcessBuilder("adb", "shell", "am", "start", "-n", "org.meow.autistic/.MainActivity").start()
        process.waitFor()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Google Sign-In / OAuth
    implementation(libs.google.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // Encrypted token storage
    implementation(libs.androidx.security.crypto)

    // DataStore for sync timestamps
    implementation(libs.androidx.datastore.preferences)

    // Gson for JSON serialization
    implementation(libs.gson)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.grpc.android)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.stub)

    // Koin DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
