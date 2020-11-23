plugins {
    application
}

val cliktVersion: String by project
val mockitoVersion: String by project
dependencies {
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    implementation(project(":okgrpc-client"))
    testImplementation(project(":okgrpc-test"))
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

application {
    mainClass.set("com.asarkar.okgrpc.OkGrpcCliKt")
}
