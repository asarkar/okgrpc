import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.google.protobuf")
}

val grpcVersion: String by project
val protobufVersion: String by project
val javaxAnnotationApiVersion: String by project
val jacksonVersion: String by project
dependencies {
    implementation(platform("com.google.protobuf:protobuf-bom:$protobufVersion"))
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion") {
        exclude("org.jetbrains.kotlin", module = "kotlin-reflect")
    }
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/${this@main.name}/grpc",
                "build/generated/source/proto/${this@main.name}/java"
            )
        }
    }
}

val bintrayUpload: com.jfrog.bintray.gradle.tasks.BintrayUploadTask by tasks

bintrayUpload.enabled = false
