package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.test.GreetingServiceGrpc
import com.asarkar.okgrpc.test.GreetingServiceImpl
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.randomStr
import io.grpc.Server
import io.grpc.protobuf.services.ProtoReflectionService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkGrpcGetCommandHandlerTest {
    private val name = randomStr()
    private val server: Server = inProcessServer(name, ProtoReflectionService.newInstance(), GreetingServiceImpl())
    private val handler = OkGrpcGetCommandHandler()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun testGetAllServices() {
        val services = handler.handleCommand(
            OkGrpcGetCommand(name)
        )
        Assertions.assertThat(services).containsExactlyInAnyOrder(
            "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService",
            "grpc.reflection.v1alpha.ServerReflection",
        )
    }

    @Test
    fun testGetMatchingServices() {
        val services = handler.handleCommand(
            OkGrpcGetCommand(name, setOf("reflection", "greeting"))
        )
        Assertions.assertThat(services).containsExactlyInAnyOrder(
            "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService",
            "grpc.reflection.v1alpha.ServerReflection",
        )
    }
}
