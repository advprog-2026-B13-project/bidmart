plugins {
    java
    id("net.serenity-bdd.serenity-gradle-plugin") version "3.6.7"
}

group = "id.ac.ui.cs.advprog"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val serenityVersion = "3.6.7"
val cucumberVersion = "7.11.2"

dependencies {
    testImplementation("net.serenity-bdd:serenity-core:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-cucumber:$serenityVersion")
    testImplementation("io.rest-assured:rest-assured:5.3.2")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.3")
    testImplementation("org.junit.platform:junit-platform-suite-api:1.9.3")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    maxHeapSize = "512m"
    finalizedBy("aggregate")
}
