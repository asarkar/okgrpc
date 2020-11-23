package com.asarkar.okgrpc

import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.randomStr
import io.grpc.Server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkGrpcClientNegativeTest {
    private val name = randomStr()
    private val server: Server = inProcessServer(name)
    private val client: OkGrpcClient = OkGrpcClient.Builder()
        .withChannel(ManagedChannelFactory.getInstance(name))
        .build()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        client.close()
        server.shutdown()
    }

    @Test
    fun testReflectionNotEnabled() {
        assertThatExceptionOfType(CancellationException::class.java).isThrownBy {
            runBlocking {
                client.listServices()
                    .toList()
            }
        }
    }
}
