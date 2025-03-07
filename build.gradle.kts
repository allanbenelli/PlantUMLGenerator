plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    
    
        implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.0")
        implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.0")
        implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.0")
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    
    
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    //jvmToolchain(17)
}