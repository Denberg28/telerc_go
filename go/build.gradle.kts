plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "io.github.denberg28.telerc.go"
 compileSdk = 35
 buildFeatures { buildConfig = true }
 defaultConfig { applicationId = "io.github.denberg28.telerc.go"; minSdk = 26; targetSdk = 35; versionCode = if (providers.gradleProperty("goUpdateFixture").orNull == "true") 1002 else 1001; versionName = if (providers.gradleProperty("goUpdateFixture").orNull == "true") "1.0.6" else "1.0.5"; testInstrumentationRunner = "io.github.denberg28.telerc.GoInstrumentation" }
 sourceSets.getByName("main").res.srcDir("../app/src/main/res")
 defaultConfig { manifestPlaceholders["goLabel"] = "TeleRC Go" }
 signingConfigs { create("releaseKey") {
  System.getenv("TELERC_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let {
   storeFile=file(it);storePassword=System.getenv("TELERC_KEYSTORE_PASSWORD")
   keyAlias=System.getenv("TELERC_KEY_ALIAS");keyPassword=System.getenv("TELERC_KEY_PASSWORD")
  }
 } }
 buildTypes {
  getByName("release") {
   // Credential-free review builds stay unsigned; configured releases retain the existing key.
   if (!System.getenv("TELERC_KEYSTORE_FILE").isNullOrBlank()) signingConfig=signingConfigs.getByName("releaseKey")
  }
  getByName("debug") {
   if (providers.gradleProperty("candidateInstall").orNull == "true") {
    applicationIdSuffix = ".candidate"
    manifestPlaceholders["goLabel"] = "TeleRC Go Candidate"
   }
  }
 }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies { testImplementation("junit:junit:4.13.2") }
