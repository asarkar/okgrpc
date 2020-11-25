package com.asarkar.okgrpc.test

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.protobuf.ByteString
import io.grpc.BindableService
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.inprocess.InProcessServerBuilder
import java.io.InputStream
import java.net.ServerSocket
import kotlin.random.Random

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

public val metadataCtxKey: Context.Key<String> = Context.key("metadata")

public fun Metadata.toJson(): String {
    return keys()
        .map {
            val key = Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)
            key.name() to this[key]
        }
        .toMap()
        .let(mapper::writeValueAsString)
}

public fun randomStr(): String {
    return generateSequence {
        val i = Random.nextInt(0, charPool.size)
        charPool[i]
    }
        .take(6)
        .joinToString("")
}

public fun inProcessServer(name: String, vararg services: BindableService): Server {
    return inProcessServer(name, emptyList(), *services)
}

public fun inProcessServer(
    name: String,
    interceptors: List<ServerInterceptor>,
    vararg services: BindableService
): Server {
    return InProcessServerBuilder.forName(name)
        .directExecutor()
        .apply {
            services.forEach { addService(it) }
            interceptors.forEach { intercept(it) }
        }
        .build()
}

public fun randomFreePort(): Int = ServerSocket(0).use { it.localPort }

public fun gRPCServer(port: Int, vararg services: BindableService): Server {
    return ServerBuilder
        .forPort(port)
        .also { services.fold(it) { a, b -> a.addService(b) } }
        .build()
}

public val mapper: JsonMapper = JsonMapper.builder().addModule(KotlinModule()).build()

public fun newPingJson(message: String): String {
    return mapper.createObjectNode()
        .put("message", message)
        .let { mapper.writeValueAsString(it) }
}

public fun newFileChunkJson(id: String, content: String, last: Boolean): String {
    return mapper.createObjectNode()
        .put("file_id", id)
        .put("content", content)
        .put("last", last)
        .let { mapper.writeValueAsString(it) }
}

public fun newGreetRequestJson(): String {
    return mapper.createObjectNode()
        .set<ObjectNode>("greeting", mapper.createObjectNode().put("name", "test"))
        .let { mapper.writeValueAsString(it) }
}

public fun newFileIdJson(id: String): String {
    return mapper.createObjectNode()
        .put("id", id)
        .let { mapper.writeValueAsString(it) }
}

public fun parsePongResponse(json: String): Pong {
    val tree = mapper.readTree(json)

    return Pong.newBuilder()
        .setMessage(tree.path("message").textValue())
        .also { builder ->
            tree.path("headers")
                .map {
                    val key = it.path("key").textValue()
                    val value = it.path("value").textValue()
                    Pair.newBuilder().setKey(key).setValue(value).build()
                }
                .also { builder.addAllHeaders(it) }
        }
        .build()
}

public fun parseGreetResponse(json: String): GreetResponse {
    val tree = mapper.readTree(json)
    return GreetResponse.newBuilder()
        .setResult(tree.path("result").textValue())
        .build()
}

public fun parseGreeting(json: String): Greeting {
    val tree = mapper.readTree(json)
    return Greeting.newBuilder()
        .setName(tree.path("greeting").path("name").textValue())
        .build()
}

public fun parseFileId(json: String): FileId {
    val tree = mapper.readTree(json)
    return FileId.newBuilder()
        .setId(tree.path("id").textValue())
        .build()
}

public fun parseFileChunk(json: String): FileChunk {
    val tree = mapper.readTree(json)
    return FileChunk.newBuilder()
        .setContent(ByteString.copyFrom(tree.path("content").textValue().encodeToByteArray()))
        .build()
}

public fun InputStream.asList(): List<ByteArray> {
    return this.use {
        generateSequence { readNBytes(512) }
            .takeWhile { it.isNotEmpty() }
            .toList()
    }
}
