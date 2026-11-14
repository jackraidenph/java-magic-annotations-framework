plugins {
    id("java")
}

group = "dev.jackraidenph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val testLibraryConsumer = sourceSets.create("testLibraryConsumer") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

sourceSets {
    test {
        compileClasspath += testLibraryConsumer.output
        runtimeClasspath += testLibraryConsumer.output
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

fun makeExports(module: String): List<String> {
    return requiredExports.map { "--add-exports".plus('=').plus(it).plus('=').plus(module) }.toList()
}

fun addExportsToCompile(module: String, vararg tasks: String) {
    tasks.forEach {
        project.tasks.named<JavaCompile>(it) {
            val exportFlags = makeExports(module)
            options.isFork = true
            options.forkOptions.jvmArgs?.addAll(exportFlags)
            options.compilerArgs.addAll(exportFlags)
        }
    }
}

addExportsToCompile("jmaf", "compileJava")
addExportsToCompile("ALL-UNNAMED", "compileTestLibraryConsumerJava", "compileTestJava")

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}