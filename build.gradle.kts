plugins {
    kotlin("jvm") version "1.9.23"
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
    reportsDirectory = layout.buildDirectory.dir("reports/jacoco")
}

group = "io.github.chr1sps"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.1.0")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = false
        csv.required = false
        html.required = true
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }
}