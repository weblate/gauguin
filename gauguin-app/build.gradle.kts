
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.triplet)
    jacoco
}

val keystoreProperties = Properties()
val keystoreExists = rootProject.file("keystore.properties").exists()

if (keystoreExists) {
    // Create a variable called keystorePropertiesFile, and initialize it to your
    // keystore.properties file, in the rootProject folder.
    val keystorePropertiesFile = rootProject.file("keystore.properties")

    // Load your keystore.properties file into the keystoreProperties object.
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    compileSdkVersion = "android-34"
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "org.piepmeyer.gauguin"
        minSdk = 24
        targetSdk = 34
        resourceConfigurations += setOf("en-rUS", "de-rDE")
    }

    if (keystoreExists) {
        signingConfigs {
            register("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    applicationVariants.all {
        this.resValue("string", "versionName", this.versionName)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            if (keystoreExists) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.txt",
            )
            resValue("bool", "debuggable", "false")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            enableUnitTestCoverage = true
            resValue("bool", "debuggable", "true")
        }

        testOptions {
            unitTests.all {
                it.useJUnitPlatform()
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        disable += "ExpiredTargetSdkVersion"
    }
    namespace = "org.piepmeyer.gauguin"
}

play {
    defaultToAppBundles.set(true)
    track.set("production")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    serviceAccountCredentials.set(file("../gauguin-serviceaccount-gradle-play-plugin.json"))
}

repositories {
    google()
    jcenter()
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

kotlin {
    jvmToolchain(11)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {

    api(project(":gauguin-core"))
    implementation(libs.koin.android)

    implementation(libs.android.material)

    implementation(libs.androidx.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.transition)
    implementation(libs.androidx.window)

    implementation(libs.thirdparty.konfetti)
    implementation(libs.thirdparty.ferriswheel)
    implementation(libs.thirdparty.navigationdrawer)
    implementation(libs.thirdparty.balloon)
    implementation(libs.thirdparty.vico)

    implementation(libs.bundles.koin)

    testImplementation(libs.bundles.kotest)
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
}

sonarqube {
    properties {
        property("sonar.androidLint.reportPaths", "$projectDir/build/reports/lint-results-debug.xml")
    }
}

tasks.create("jacocoUnitTestReport", JacocoReport::class.java) {
    // dependsOn(tasks.named("testDebugUnitTest"))

    reports {
        csv.required = false
        xml.required = true
        html.required = true
    }
}
