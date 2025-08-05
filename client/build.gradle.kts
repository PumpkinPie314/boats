// fallowing https://gradleup.com/shadow/getting-started/#__tabbed_1_1 for fat jars
import org.gradle.internal.os.OperatingSystem
plugins {
    java
    application
  	id("com.gradleup.shadow") version "8.3.8"
}
buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    classpath("com.gradleup.shadow:shadow-gradle-plugin:8.3.8")
  }
}
apply(plugin = "java")
apply(plugin = "com.gradleup.shadow")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("game.client.Main")
}

// the below is mostly generated from https://www.lwjgl.org/customize

val lwjglVersion = "3.3.6"

val lwjglNatives = project.findProperty("targetPlatform")?.toString() ?: Pair(
	System.getProperty("os.name")!!,
	System.getProperty("os.arch")!!
).let { (name, arch) ->
	when {
		arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
			if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
				"natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
			else if (arch.startsWith("ppc"))
				"natives-linux-ppc64le"
			else if (arch.startsWith("riscv"))
				"natives-linux-riscv64"
			else
				"natives-linux"
		arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }     ->
			"natives-macos"
		arrayOf("Windows").any { name.startsWith(it) }                ->
			"natives-windows"
		else                                                                            ->
			throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
	}
}

repositories {
    mavenCentral()
}


dependencies {
	implementation(project(":common"))
	implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

	implementation("org.lwjgl", "lwjgl")
	implementation("org.lwjgl", "lwjgl-assimp")
	implementation("org.lwjgl", "lwjgl-bgfx")
	implementation("org.lwjgl", "lwjgl-glfw")
	implementation("org.lwjgl", "lwjgl-nanovg")
	implementation("org.lwjgl", "lwjgl-nuklear")
	implementation("org.lwjgl", "lwjgl-openal")
	implementation("org.lwjgl", "lwjgl-opengl")
	implementation("org.lwjgl", "lwjgl-par")
	implementation("org.lwjgl", "lwjgl-stb")
	implementation("org.lwjgl", "lwjgl-vulkan")
	runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-bgfx", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-nanovg", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-nuklear", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-par", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
	if (lwjglNatives == "natives-macos") implementation ("org.lwjgl", "lwjgl-vulkan", classifier = lwjglNatives)
	
	implementation("org.joml:joml:1.10.8") // added for vectors and math! 
}
