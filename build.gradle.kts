plugins {
    id("java")
}

group = "dev.jackraidenph"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val optsCompile = listOf(
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.main=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.tree=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.processing=jmaf",
    "--add-exports=jdk.compiler/com.sun.tools.javac.model=jmaf"
)

val optsRun = listOf(
    "--add-opens=jdk.compiler/com.sun.tools.javac.code=jmaf",
    "--add-opens=jdk.compiler/com.sun.tools.javac.comp=jmaf"
)

tasks.named<JavaCompile>("compileTestJava") {
    outputs.upToDateWhen { false }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.jvmArgs?.addAll(optsCompile)
    options.compilerArgs.addAll(optsCompile)
}

tasks.withType<Test>().configureEach {
    jvmArgs(optsCompile)
    jvmArgs(optsRun)
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