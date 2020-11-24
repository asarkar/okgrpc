plugins {
    id("com.github.johnrengelman.shadow")
}

val cliktVersion: String by project
val mockitoVersion: String by project
dependencies {
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    implementation(project(":okgrpc-client"))
    testImplementation(project(":okgrpc-test"))
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

project.tasks.findByName("jar")?.enabled = false

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveClassifier.set("")
    manifest {
        attributes(mapOf("Main-Class" to "com.asarkar.okgrpc.OkGrpcCliKt"))
    }
}
