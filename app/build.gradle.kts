import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Friends feature: the Firebase project's public client settings (see docs/FRIENDS_SETUP.md).
 * Read from `app/google-services.json` as downloaded from the Firebase console, or else from
 * `firebase.properties` in the repository root. Both are git-ignored; without either the app still
 * builds and Friends shows "not set up". The Google Services Gradle plugin isn't used: the values
 * are compiled into BuildConfig and Firebase is initialised in code.
 */
@Suppress("UNCHECKED_CAST")
val firebaseValues: Map<String, String> = run {
    val json = file("google-services.json")
    if (json.exists()) {
        val root = groovy.json.JsonSlurper().parse(json) as Map<String, Any?>
        val project = root["project_info"] as? Map<String, Any?> ?: emptyMap()
        val clients = (root["client"] as? List<Map<String, Any?>>).orEmpty()
        val client = clients.firstOrNull {
            ((it["client_info"] as? Map<String, Any?>)?.get("android_client_info") as? Map<String, Any?>)?.get("package_name") == "com.attendo.app"
        } ?: clients.firstOrNull() ?: emptyMap()
        val apiKey = (client["api_key"] as? List<Map<String, Any?>>)?.firstOrNull()?.get("current_key") as? String
        val oauth = (client["oauth_client"] as? List<Map<String, Any?>>).orEmpty() +
            (((client["services"] as? Map<String, Any?>)?.get("appinvite_service") as? Map<String, Any?>)
                ?.get("other_platform_oauth_client") as? List<Map<String, Any?>>).orEmpty()
        mapOf(
            "projectId" to (project["project_id"] as? String).orEmpty(),
            "appId" to ((client["client_info"] as? Map<String, Any?>)?.get("mobilesdk_app_id") as? String).orEmpty(),
            "apiKey" to apiKey.orEmpty(),
            // Client type 3 = the project's "Web client", whose ID Google sign-in needs for Firebase.
            "webClientId" to (oauth.firstOrNull { (it["client_type"] as? Number)?.toInt() == 3 }?.get("client_id") as? String).orEmpty(),
        )
    } else {
        val props = Properties().apply {
            rootProject.file("firebase.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        listOf("projectId", "appId", "apiKey", "webClientId").associateWith { props.getProperty(it, "").trim() }
    }
}

fun firebaseValue(key: String): String = "\"" + firebaseValues[key].orEmpty().replace("\"", "") + "\""

android {
    namespace = "com.attendo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.attendo.app"
        buildConfigField("String", "FIREBASE_PROJECT_ID", firebaseValue("projectId"))
        buildConfigField("String", "FIREBASE_APP_ID", firebaseValue("appId"))
        buildConfigField("String", "FIREBASE_API_KEY", firebaseValue("apiKey"))
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", firebaseValue("webClientId"))
        minSdk = 26
        targetSdk = 34
        versionCode = 32
        versionName = "0.31.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No release keystore yet: sign with the debug key so the APK is installable for testing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:backup"))
    implementation(project(":core:social"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:attendance"))
    implementation(project(":feature:subjects"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:splash"))
    implementation(project(":feature:pomodoro"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:tasks"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:curriculum"))
    implementation(project(":feature:friends"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
}
