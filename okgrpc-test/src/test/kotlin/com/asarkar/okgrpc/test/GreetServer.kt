package com.asarkar.okgrpc.test

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import org.slf4j.LoggerFactory
import java.net.ServerSocket

private val logger = LoggerFactory.getLogger("com.asarkar.okgrpc.test.GreetingServer")

fun main() {
    val randomFreePort = ServerSocket(0).use { it.localPort }
    val server = ServerBuilder
        .forPort(randomFreePort)
        .addService(ProtoReflectionService.newInstance())
        .addService(GreetingServiceImpl())
        .build()

    logger.info("Starting Greeting server on port: {}", randomFreePort)
    server.start()
    server.awaitTermination()
}
