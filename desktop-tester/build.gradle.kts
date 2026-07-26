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

tasks.register<JavaExec>("runIdCard") {
    group = "verification"
    description = "Runs ID-card detection and PDF generation against test-images/card_test_* fixtures."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.oscan.desktop.IdCardMainKt")
    workingDir = rootDir
}
