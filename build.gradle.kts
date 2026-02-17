plugins {
    java
}

group = "com.werchat"
version = "1.1.8"

java {
    // Note: Compiling with Java 25, targeting Java 21 bytecode for Hytale compatibility
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://maven.hytale.com/release/")
    maven("https://repo.helpch.at/releases")
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:latest.release")
    compileOnly("at.helpch:placeholderapi-hytale:1.0.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.werchat.WerchatPlugin"
        )
    }
    // Include Gson in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Auto-deploy to Hytale Mods folder after build
tasks.register<Copy>("deploy") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into(System.getenv("APPDATA") + "/Hytale/UserData/Mods")
    doLast {
        println("Deployed to Hytale Mods folder!")
    }
}

// Make build also deploy
tasks.build {
    finalizedBy("deploy")
}
