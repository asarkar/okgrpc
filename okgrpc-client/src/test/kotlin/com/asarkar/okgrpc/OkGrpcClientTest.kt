package com.asarkar.okgrpc

import com.asarkar.okgrpc.test.FileServiceImpl
import com.asarkar.okgrpc.test.FileServiceProto
import com.asarkar.okgrpc.test.GreetingProto
import com.asarkar.okgrpc.test.GreetingServiceGrpc
import com.asarkar.okgrpc.test.GreetingServiceImpl
import com.asarkar.okgrpc.test.GreetingServiceProto
import com.asarkar.okgrpc.test.asList
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.newFileChunkJson
import com.asarkar.okgrpc.test.newFileIdJson
import com.asarkar.okgrpc.test.newGreetRequestJson
import com.asarkar.okgrpc.test.parseFileChunk
import com.asarkar.okgrpc.test.parseFileId
import com.asarkar.okgrpc.test.parseGreetResponse
import com.asarkar.okgrpc.test.randomStr
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.Server
import io.grpc.protobuf.services.ProtoReflectionService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkGrpcClientTest {
    private val name = randomStr()
    private val server: Server = inProcessServer(
        name,
        ProtoReflectionService.newInstance(),
        GreetingServiceImpl(),
        FileServiceImpl()
    )
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
    fun testListServices() {
        val services = runBlocking {
            client.listServices()
                .toList()
        }
        assertThat(services).containsExactlyInAnyOrder(
            "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService",
            "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.FileService",
            "grpc.reflection.v1alpha.ServerReflection",
        )
    }

    @Test
    fun testFindProtoByServiceName() {
        val proto = runBlocking {
            client.findProtoBySymbol("${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService")
        }
        assertThat(proto.`package`).isEqualTo(GreetingProto::class.java.packageName)
        assertThat(proto.serviceCount).isEqualTo(1)
        val greetingService = proto.serviceList.first()
        assertThat(greetingService.methodCount).isEqualTo(4)
        assertThat(greetingService.methodList.map { it.name }).containsExactlyInAnyOrder(
            "Greet",
            "GreetManyTimes",
            "LongGreet",
            "GreetEveryone"
        )
    }

    @Test
    fun testFindProtoByMethodName() {
        val proto = runBlocking {
            client.findProtoBySymbol("${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.Greet")
        }
        assertThat(proto.`package`).isEqualTo(GreetingProto::class.java.packageName)
        assertThat(proto.serviceCount).isEqualTo(1)
        val greetingService = proto.serviceList.first()
        assertThat(greetingService.methodCount).isEqualTo(4)
        assertThat(greetingService.methodList.map { it.name }).containsExactlyInAnyOrder(
            "Greet",
            "GreetManyTimes",
            "LongGreet",
            "GreetEveryone"
        )
    }

    @Test
    fun testFindProtoByTypeName() {
        val proto = runBlocking {
            client.findProtoBySymbol("${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetRequest")
        }
        assertThat(proto.`package`).isEqualTo(GreetingProto::class.java.packageName)
        assertThat(proto.serviceList).isEmpty()
        assertThat(proto.messageTypeList.map { it.name }).containsExactlyInAnyOrder(
            "Greeting",
            "GreetRequest",
            "GreetResponse"
        )
    }

    @Test
    fun testListServiceProtos() {
        val protos = runBlocking {
            client.listServiceProtos()
                .toList()
        }
        assertThat(protos).hasSize(4)
        assertThat(protos).anyMatch { it.name == GreetingServiceProto.getDescriptor().name }
        assertThat(protos).anyMatch { it.name == GreetingProto.getDescriptor().name }
        assertThat(protos).anyMatch { it.name == "grpc/reflection/v1alpha/reflection.proto" }
        assertThat(protos).anyMatch { it.name == FileServiceProto.getDescriptor().name }
    }

    @Test
    fun testUnaryCall() {
        val responses = runBlocking {
            client.exchange(
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.Greet",
                flowOf(newGreetRequestJson())
            )
                .toList()
        }
        assertThat(responses).hasSize(1)
        assertThat(parseGreetResponse(responses.first())).isEqualTo("Hello, test")
    }

    @Test
    fun testServerStreamingCall() {
        val responses = runBlocking {
            client.exchange(
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.GreetManyTimes",
                flowOf(newGreetRequestJson())
            )
                .toList()
        }
        assertThat(responses).hasSize(3)
        assertThat(responses.zip(1..3)).allMatch { pair ->
            parseGreetResponse(pair.first) == "Hello, test[${pair.second}]"
        }
    }

    @Test
    fun testClientStreamingCall() {
        val responses = runBlocking {
            client.exchange(
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.LongGreet",
                flow {
                    repeat(3) {
                        emit(newGreetRequestJson())
                    }
                }
            )
                .toList()
        }
        assertThat(responses).hasSize(1)
        assertThat(parseGreetResponse(responses.first()))
            .isEqualTo("Hello, test! Hello, test! Hello, test")
    }

    @Test
    fun testBidiStreamingCall() {
        val responses = runBlocking {
            client.exchange(
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.GreetEveryone",
                flow {
                    repeat(3) {
                        emit(newGreetRequestJson())
                    }
                }
            )
                .toList()
        }
        assertThat(responses).hasSize(3)
        assertThat(responses)
            .allMatch { parseGreetResponse(it) == "Hello, test" }
    }

    @Test
    fun testNonExistentMethod() {
        Assertions.assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            runBlocking {
                client.exchange(
                    "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.DoesNotExist",
                    flowOf(newGreetRequestJson())
                )
                    .toList()
            }
        }
    }

    @Test
    fun testMalformedInput() {
        Assertions.assertThatExceptionOfType(InvalidProtocolBufferException::class.java).isThrownBy {
            runBlocking {
                client.exchange(
                    "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.Greet",
                    flowOf("""{ "greeting": { "name": "test" } """)
                )
                    .toList()
            }
        }
    }

    @Test
    fun testUploadDownload() {
        val fileId = "grpc-logo.png"
        val chunks1 = javaClass.getResourceAsStream("/$fileId")
            .asList()
        val encoder = Base64.getEncoder()
        val responses = runBlocking {
            client.exchange(
                GrpcMethod(
                    GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName,
                    "FileService",
                    "Upload"
                ),
                chunks1.zip(1..chunks1.size).map {
                    val encoded = encoder.encodeToString(it.first)
                    newFileChunkJson(fileId, encoded, it.second == chunks1.size)
                }
                    .asFlow()
            )
                .toList()
        }
        assertThat(responses).hasSize(1)
        val id = parseFileId(responses.first())
        assertThat(id).isEqualTo(fileId)

        val chunks2 = runBlocking {
            client.exchange(
                GrpcMethod(
                    GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName,
                    "FileService",
                    "Download"
                ),
                flowOf(newFileIdJson(fileId))
            )
                .toList()
        }
        val decoder = Base64.getDecoder()
        assertThat(chunks2.zip(chunks2.indices)).allMatch {
            val decoded = decoder.decode(parseFileChunk(it.first))
            decoded.contentEquals(chunks1[it.second])
        }
    }
}
