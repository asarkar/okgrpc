package com.asarkar.okgrpc.test

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import io.grpc.Metadata

public val mapper: JsonMapper = JsonMapper.builder().addModule(KotlinModule()).build()
private val printer: JsonFormat.Printer = JsonFormat.printer()
public val parser: JsonFormat.Parser = JsonFormat.parser()

public fun Metadata.toJson(): String {
    return keys()
        .map {
            val key = Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)
            key.name() to this[key]
        }
        .toMap()
        .let(mapper::writeValueAsString)
}

public fun newPingJson(message: String): String {
    return printer.print(
        Ping.newBuilder()
            .setMessage(message)
            .build()
    )
}

public fun newFileChunkJson(id: String, content: String, last: Boolean): String {
    return printer.print(
        FileChunk.newBuilder()
            .setFileId(id)
            .setContent(ByteString.copyFromUtf8(content))
            .setLast(last)
            .build()
    )
}

public fun newGreetRequestJson(): String {
    return printer.print(
        GreetRequest.newBuilder()
            .setGreeting(Greeting.newBuilder().setName("test").build())
            .build()
    )
}

public fun newFileIdJson(id: String): String {
    return printer.print(
        FileId.newBuilder()
            .setId(id)
            .build()
    )
}

public inline fun <reified T> parse(json: String): T {
    val builder = T::class.java.getDeclaredMethod("newBuilder").invoke(null) as Message.Builder
    parser.merge(json, builder)
    return builder.build() as T
}
