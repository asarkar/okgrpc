package com.asarkar.okgrpc.test

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.inprocess.InProcessServerBuilder
import java.io.InputStream
import java.net.ServerSocket
import kotlin.random.Random

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

public fun randomStr(): String {
    return generateSequence {
        val i = Random.nextInt(0, charPool.size)
        charPool[i]
    }
        .take(6)
        .joinToString("")
}

public fun inProcessServer(name: String, vararg services: BindableService): Server {
    return InProcessServerBuilder.forName(name)
        .directExecutor()
        .also { services.fold(it) { a, b -> a.addService(b) } }
        .build()
}

public fun randomFreePort(): Int = ServerSocket(0).use { it.localPort }

public fun gRPCServer(port: Int, vararg services: BindableService): Server {
    return ServerBuilder
        .forPort(port)
        .also { services.fold(it) { a, b -> a.addService(b) } }
        .build()
}

private val mapper = JsonMapper.builder().addModule(KotlinModule()).build()

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

public fun parseGreetResponse(json: String): String {
    return mapper.readTree(json)
        .path("result")
        .textValue()
}

public fun parseGreeting(json: String): String {
    return mapper.readTree(json)
        .path("greeting")
        .path("name")
        .textValue()
}

public fun parseFileId(json: String): String {
    return mapper.readTree(json)
        .path("id").textValue()
}

public fun parseFileChunk(json: String): String {
    return mapper.readTree(json)
        .path("content").textValue()
}

public fun InputStream.asList(): List<ByteArray> {
    return this.use {
        generateSequence { readNBytes(512) }
            .takeWhile { it.isNotEmpty() }
            .toList()
    }
}
