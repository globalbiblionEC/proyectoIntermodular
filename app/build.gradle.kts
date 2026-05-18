plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //PLUGIN PARA FIREBASE
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.globalbiblion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.globalbiblion"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

 
    //FIREBASE + AUTH KTX SIN VERSIÓN
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    //implementation(platform("com.google.firebase:firebase-bom:32.6.0"))//ANTESSSSSSS
    implementation("com.google.firebase:firebase-auth-ktx")
    //HE AÑADIDO LA LINEA DE ABAJO
    implementation("com.google.firebase:firebase-firestore-ktx")
    //FirebaseStorage
    implementation("com.google.firebase:firebase-storage")
    //Glide para optimizar la carga de imagenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    //Para exportar los datos del usuario
    implementation("com.google.firebase:firebase-functions-ktx")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)}
