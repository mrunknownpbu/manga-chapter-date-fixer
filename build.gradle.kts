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
    
    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core:2.3.6")
    implementation("io.ktor:ktor-server-netty:2.3.6")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-jackson:2.3.6")
    implementation("io.ktor:ktor-server-cors:2.3.6")
}

sourceSets {
    main {
        kotlin {
            srcDirs("src")
        }
    }
}

application {
    mainClass.set("MainKt")
}

// Create a fat JAR with all dependencies
tasks.jar {
    archiveBaseName.set("manga-chapter-date-fixer")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    manifest {
        attributes(
            "Main-Class" to "MainKt"
        )
    }
}