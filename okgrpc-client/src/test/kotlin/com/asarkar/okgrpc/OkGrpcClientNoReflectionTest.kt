package com.asarkar.okgrpc

import com.asarkar.okgrpc.test.GreetResponse
import com.asarkar.okgrpc.test.GreetingServiceGrpc
import com.asarkar.okgrpc.test.GreetingServiceImpl
import com.asarkar.okgrpc.test.GreetingServiceProto
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.newGreetRequestJson
import com.asarkar.okgrpc.test.parse
import com.asarkar.okgrpc.test.randomStr
import io.grpc.Server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths
import kotlin.io.path.name

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkGrpcClientNoReflectionTest {
    private val name = randomStr()
    private val server: Server = inProcessServer(name, GreetingServiceImpl())
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

    private val rootDir = Paths.get(OkGrpcClientNoReflectionTest::class.java.protectionDomain.codeSource.location.toURI())
        .let { testPath ->
            generateSequence(testPath) { it.parent }
                .dropWhile { it.name != "okgrpc" }
                .take(1)
                .first()
        }

    @Test
    fun testReflectionNotEnabled() {
        Assertions.assertThatExceptionOfType(CancellationException::class.java).isThrownBy {
            runBlocking {
                client.listServices()
                    .toList()
            }
        }
    }

    @Test
    fun testUnaryCall() {
        val responses = runBlocking {
            client.exchange(
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.Greet",
                flowOf(newGreetRequestJson()),
                protoPaths = listOf(rootDir.resolve("okgrpc-test/src/main/proto").toString()),
                protoFile = GreetingServiceProto.getDescriptor().name
            )
                .toList()
        }
        Assertions.assertThat(responses).hasSize(1)
        Assertions.assertThat(parse<GreetResponse>(responses.first()).result).isEqualTo("Hello, test")
    }
}
