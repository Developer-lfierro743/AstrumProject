plugins {
    kotlin("jvm") version "2.3.20"
    application
}

group = "com.novusforge.astrum"
version = "0.1.0-SNAPSHOT"

val lwjglVersion = "3.4.1"
val jomlVersion = "1.10.8"
val lwjglNatives = "natives-linux-arm64"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    
    // LWJGL
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-shaderc:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-vma:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-assimp:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    
    // Natives
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
    
    // Vulkan Bootstrap
    implementation("com.github.YvesBoyadjian:vk-bootstrap4j:master-SNAPSHOT") {
        exclude(group = "org.lwjgl")
    }
    
    // JOML
    implementation("org.joml:joml:$jomlVersion")
}

// Kotlin sources in separate directory
sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
    }
}

kotlin {
    jvmToolchain(21)
}

// Kotlin automatically sees Java classes - no explicit dependency needed
application {
    mainClass.set("com.novusforge.astrum.game.MainKt")
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "-Xmx512m", "-Xms128m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=50",
        "--enable-native-access=ALL-UNNAMED",
        "-Djava.library.path=/usr/lib/aarch64-linux-gnu"
    )
    environment(
        "LD_LIBRARY_PATH" to (System.getenv("LD_LIBRARY_PATH") ?: "") + ":/usr/lib/aarch64-linux-gnu",
        "VK_ICD_FILENAMES" to "/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json"
    )
}

tasks.test {
    useJUnitPlatform()
}
