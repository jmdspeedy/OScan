plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // OpenCV Desktop via OpenPnP
    implementation("org.openpnp:opencv:4.9.0-0")

    // Offline DocQuadNet inference for difficult low-contrast/textured scenes.
    implementation("com.microsoft.onnxruntime:onnxruntime:1.24.1")
    
    // Apache PDFBox for PDF Generation
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
}
