package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.test.GreetingServiceGrpc
import com.asarkar.okgrpc.test.GreetingServiceImpl
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.newGreetRequestJson
import com.asarkar.okgrpc.test.parseGreetResponse
import com.asarkar.okgrpc.test.randomStr
import io.grpc.Server
import io.grpc.protobuf.services.ProtoReflectionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkGrpcExecCommandHandlerTest {
    private val name = randomStr()
    private val server: Server = inProcessServer(name, ProtoReflectionService.newInstance(), GreetingServiceImpl())
    private val handler = OkGrpcExecCommandHandler()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun testExecUnary() {
        val responses = handler.handleCommand(
            OkGrpcExecCommand(
                name,
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.Greet",
                listOf(newGreetRequestJson())
            )
        )
        assertThat(responses).hasSize(1)
        assertThat(parseGreetResponse(responses.first())).isEqualTo("Hello, test")
    }
}
