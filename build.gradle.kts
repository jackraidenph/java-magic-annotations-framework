plugins {
    id("java")
}

group = "dev.jackraidenph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val commonForTests = sourceSets.create("commonForTests") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

sourceSets {
    test {
        compileClasspath += commonForTests.output
        runtimeClasspath += commonForTests.output
    }
}

val requiredExports = listOf(
    "jdk.compiler/com.sun.tools.javac.api",
    "jdk.compiler/com.sun.tools.javac.main",
    "jdk.compiler/com.sun.tools.javac.tree",
    "jdk.compiler/com.sun.tools.javac.util",
    "jdk.compiler/com.sun.tools.javac.processing",
    "jdk.compiler/com.sun.tools.javac.model"
)

tasks.named<JavaCompile>("compileJava") {
    val exportFlags = requiredExports.map { "--add-exports".plus('=').plus(it).plus("=jmaf") }.toList()
    options.isFork = true
    options.forkOptions.jvmArgs?.addAll(exportFlags)
    options.compilerArgs.addAll(exportFlags)
}

listOf("compileCommonForTestsJava", "compileTestJava").forEach {
    tasks.named<JavaCompile>(it) {
        val exportFlags = requiredExports.map { "--add-exports".plus('=').plus(it).plus("=ALL-UNNAMED") }.toList()
        options.isFork = true
        options.forkOptions.jvmArgs?.addAll(exportFlags)
        options.compilerArgs.addAll(exportFlags)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}