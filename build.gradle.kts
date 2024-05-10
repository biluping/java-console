plugins {
    id("java")
}

group = "org.myboy"
version = "17"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("cn.hutool:hutool-all:5.8.26")
    implementation("io.javalin:javalin:6.1.3")
    implementation("io.github.liubsyy:FindInstancesOfClass:1.0.3")
    implementation("org.codehaus.groovy:groovy-all:3.0.21")
    implementation("org.jline:jline:3.26.1")
    implementation("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "com.myboy.Attach",
            "Agent-Class" to "com.myboy.JavaConsoleAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
}