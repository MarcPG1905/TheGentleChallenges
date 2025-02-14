plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

group = "com.marcpg"
version = "0.1.0"
description = "Challenge plugin for TheGentleCrafter, made my MarcPG."

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases/")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    implementation("com.marcpg:libpg-paper:1.0.0")
    implementation("com.marcpg:libpg-storage-json:1.0.0")
    compileOnly("xyz.xenondevs.invui:invui:1.44")
}

tasks {
    reobfJar {
        dependsOn(jar)
    }
    build {
        dependsOn(shadowJar)
        dependsOn(reobfJar)
    }
    runServer {
        dependsOn(shadowJar)
        minecraftVersion("1.21.4")
    }
    shadowJar {
        archiveClassifier.set("")
    }
    processResources {
        filter {
            it.replace("\${version}", version.toString())
        }
    }
}
