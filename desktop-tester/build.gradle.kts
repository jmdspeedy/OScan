plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.oscan.desktop.MainKt")
}

dependencies {
    implementation(project(":core-engine"))
    implementation("org.openpnp:opencv:4.9.0-0")
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
