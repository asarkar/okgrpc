package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.test.EchoServiceGrpc
import com.asarkar.okgrpc.test.EchoServiceImpl
import com.asarkar.okgrpc.test.MetadataTransferringServerInterceptor
import com.asarkar.okgrpc.test.Pong
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.newPingJson
import com.asarkar.okgrpc.test.parse
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
    private val server: Server = inProcessServer(
        name,
        listOf(MetadataTransferringServerInterceptor()),
        ProtoReflectionService.newInstance(),
        EchoServiceImpl()
    )
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
    fun testExec() {
        val responses = handler.handleCommand(
            OkGrpcExecCommand(
                name,
                "${EchoServiceGrpc.EchoServiceImplBase::class.java.packageName}.EchoService.Echo",
                listOf(newPingJson("test")),
                headers = mapOf("key" to "value")
            )
        )
        assertThat(responses).hasSize(1)
        val pong = parse<Pong>(responses.first())
        assertThat(pong.message).isEqualTo("test")
        assertThat(pong.headersList).isNotEmpty
        assertThat(pong.headersList).anyMatch { it.key == "key" && it.value == "value" }
    }
}
