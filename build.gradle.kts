plugins {
    id("java")
}

group = "dev.jackraidenph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val optsCompileModule = listOf(
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.main=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.tree=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.processing=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.model=jmaf"
)

val optsCompile = listOf(
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED"
)

tasks.named<JavaCompile>("compileTestJava") {
    outputs.upToDateWhen { false }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.jvmArgs?.addAll(optsCompile)
    options.compilerArgs.addAll(optsCompile)
    options.compilerArgs.addAll(optsCompileModule)
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

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}