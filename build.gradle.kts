// Root build file for the finalproject
// This is a multi-module Gradle project with client and server subprojects

plugins {
    // Apply the java plugin to add support for Java
    java
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}
