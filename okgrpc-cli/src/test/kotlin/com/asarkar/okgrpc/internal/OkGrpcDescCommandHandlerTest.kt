package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.test.GreetingServiceGrpc
import com.asarkar.okgrpc.test.GreetingServiceImpl
import com.asarkar.okgrpc.test.inProcessServer
import com.asarkar.okgrpc.test.randomStr
import io.grpc.Server
import io.grpc.protobuf.services.ProtoReflectionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkGrpcDescCommandHandlerTest {
    private val name = randomStr()
    private val server: Server = inProcessServer(name, ProtoReflectionService.newInstance(), GreetingServiceImpl())
    private val handler = OkGrpcDescCommandHandler()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun testDescribeService() {
        val protos = handler.handleCommand(
            OkGrpcDescCommand(
                DescKind.SERVICE,
                name,
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService"
            )
        )
        assertThat(protos).hasSize(1)
        assertThat(protos.first()).isEqualTo(
            """
            name: "com/asarkar/okgrpc/test/greeting_service.proto"
            package: "com.asarkar.okgrpc.test"
            dependency: "com/asarkar/okgrpc/test/greeting.proto"
            service {
              name: "GreetingService"
              method {
                name: "Greet"
                input_type: ".com.asarkar.okgrpc.test.GreetRequest"
                output_type: ".com.asarkar.okgrpc.test.GreetResponse"
                options {
                }
              }
              method {
                name: "GreetManyTimes"
                input_type: ".com.asarkar.okgrpc.test.GreetRequest"
                output_type: ".com.asarkar.okgrpc.test.GreetResponse"
                options {
                }
                server_streaming: true
              }
              method {
                name: "LongGreet"
                input_type: ".com.asarkar.okgrpc.test.GreetRequest"
                output_type: ".com.asarkar.okgrpc.test.GreetResponse"
                options {
                }
                client_streaming: true
              }
              method {
                name: "GreetEveryone"
                input_type: ".com.asarkar.okgrpc.test.GreetRequest"
                output_type: ".com.asarkar.okgrpc.test.GreetResponse"
                options {
                }
                client_streaming: true
                server_streaming: true
              }
            }
            options {
              java_outer_classname: "GreetingServiceProto"
              java_multiple_files: true
            }
            syntax: "proto3"
            
            """.trimIndent()
        )
    }

    @Test
    fun testDescribeMethod() {
        val protos = handler.handleCommand(
            OkGrpcDescCommand(
                DescKind.METHOD,
                name,
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetingService.Greet"
            )
        )
        assertThat(protos).hasSize(1)
        assertThat(protos.first()).isEqualTo(
            """
            name: "Greet"
            input_type: ".com.asarkar.okgrpc.test.GreetRequest"
            output_type: ".com.asarkar.okgrpc.test.GreetResponse"
            options {
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun testDescribeType() {
        val protos = handler.handleCommand(
            OkGrpcDescCommand(
                DescKind.TYPE,
                name,
                "${GreetingServiceGrpc.GreetingServiceImplBase::class.java.packageName}.GreetRequest"
            )
        )
        assertThat(protos).hasSize(1)
        assertThat(protos.first()).isEqualTo(
            """
            name: "GreetRequest"
            field {
              name: "greeting"
              number: 1
              label: LABEL_OPTIONAL
              type: TYPE_MESSAGE
              type_name: ".com.asarkar.okgrpc.test.Greeting"
            }
            
            """.trimIndent()
        )
    }
}
