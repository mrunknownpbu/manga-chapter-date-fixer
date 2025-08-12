plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.yaml:snakeyaml:2.2")
    // For HTTP requests (add your favorite: Ktor, OkHttp, etc.)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

application {
    mainClass.set("MainKt")
}