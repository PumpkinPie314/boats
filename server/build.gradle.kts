plugins {
    java
    application
}
repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("game.server.Main") 
}
dependencies {
	implementation("org.joml:joml:1.10.8")
}
