package com.asarkar.okgrpc.test

import io.grpc.protobuf.services.ProtoReflectionService
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.asarkar.okgrpc.test.GreetingServer")

fun main() {
    val port = randomFreePort()
    val server = gRPCServer(port, ProtoReflectionService.newInstance(), GreetingServiceImpl())
    logger.info("Starting Greeting server on port: {}", port)
    server.start()
    server.awaitTermination()
}
