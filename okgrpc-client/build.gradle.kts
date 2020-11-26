val grpcKotlinVersion: String by project
val protobufVersion: String by project
val protocJarVersion: String by project
dependencies {
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("com.github.os72:protoc-jar:$protocJarVersion")
    testImplementation(project(":okgrpc-test"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            // Used by Flow builders
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            // Used by stub deadlines
            "-Xopt-in=kotlin.time.ExperimentalTime",
            // Used by test to find project root dir
            "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
        )
    }
}
