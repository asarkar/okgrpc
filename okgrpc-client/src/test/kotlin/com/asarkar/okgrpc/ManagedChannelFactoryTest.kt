package com.asarkar.okgrpc

import com.asarkar.okgrpc.test.gRPCServer
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.randomFreePort
import com.asarkar.okgrpc.test.randomStr
import io.grpc.ManagedChannel
import io.grpc.Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ManagedChannelFactoryTest {
    lateinit var server: Server
    lateinit var channel: ManagedChannel

    @AfterEach
    fun afterEach() {
        channel.shutdown()
        server.shutdown()
    }

    @Test
    fun testGetInProcessServerInstance() {
        val name = randomStr()

        server = inProcessServer(name)
        server.start()
        channel = ManagedChannelFactory.getInstance(name)
        assertThat(channel.authority()).isEqualTo("localhost")
    }

    @Test
    fun testGetNettyServerInstance() {
        val port = randomFreePort()
        server = gRPCServer(port)
        server.start()
        channel = ManagedChannelFactory.getInstance("localhost:$port")
        assertThat(channel.authority()).isEqualTo("localhost:$port")
    }
}
