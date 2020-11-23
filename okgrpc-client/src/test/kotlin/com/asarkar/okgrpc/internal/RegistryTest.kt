package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.test.AnotherReflectableServiceGrpc
import com.asarkar.okgrpc.test.EmptyMessage
import com.asarkar.okgrpc.test.GreetRequest
import com.asarkar.okgrpc.test.Greeting
import com.asarkar.okgrpc.test.GreetingProto
import com.asarkar.okgrpc.test.NestedTypeOuter
import com.asarkar.okgrpc.test.ReflectableServiceGrpc
import com.asarkar.okgrpc.test.ReflectionTestDepthThreeProto
import com.asarkar.okgrpc.test.ReflectionTestDepthTwoAlternateProto
import com.asarkar.okgrpc.test.ReflectionTestDepthTwoProto
import com.asarkar.okgrpc.test.ReflectionTestProto
import com.asarkar.okgrpc.test.Reply
import com.asarkar.okgrpc.test.Request
import com.asarkar.okgrpc.test.ThirdLevelType
import com.asarkar.okgrpc.test.ThirdLevelTypeSubMsg1
import com.asarkar.okgrpc.test.ThirdLevelTypeSubMsg2
import com.asarkar.okgrpc.test.parseGreeting
import com.google.protobuf.Any
import com.google.protobuf.AnyProto
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegistryTest {
    @Test
    fun testReflectionTestDepthThreeProto() {
        val registry = Registry.fromProtos(
            listOf(
                ReflectionTestDepthThreeProto.getDescriptor().toProto(),
                AnyProto.getDescriptor().toProto()
            )
        )
        assertThat(registry.methodRegistry.methodMap).isEmpty()
        // TODO: Should include nested types?
        assertThat(registry.typeRegistry.messageDescriptors.map { it.name }).containsExactlyInAnyOrder(
            ThirdLevelType.getDescriptor().name,
            NestedTypeOuter.getDescriptor().name,
            EmptyMessage.getDescriptor().name,
            Any.getDescriptor().name
        )
    }

    @Test
    fun testReflectionTestDepthTwoProto() {
        val registry = Registry.fromProtos(
            listOf(
                ReflectionTestDepthTwoProto.getDescriptor().toProto(),
                ReflectionTestDepthThreeProto.getDescriptor().toProto(),
                AnyProto.getDescriptor().toProto()
            )
        )
        assertThat(registry.methodRegistry.methodMap).isEmpty()
        assertThat(registry.typeRegistry.messageDescriptors.map { it.name }).containsExactlyInAnyOrder(
            ThirdLevelType.getDescriptor().name,
            NestedTypeOuter.getDescriptor().name,
            EmptyMessage.getDescriptor().name,
            Any.getDescriptor().name,
            Request.getDescriptor().name,
            Reply.getDescriptor().name,
            ThirdLevelTypeSubMsg1.getDescriptor().name
        )
    }

    @Test
    fun testReflectionTestDepthTwoAlternateProto() {
        val registry = Registry.fromProtos(
            listOf(
                ReflectionTestDepthTwoAlternateProto.getDescriptor().toProto(),
                ReflectionTestDepthThreeProto.getDescriptor().toProto(),
                AnyProto.getDescriptor().toProto()
            )
        )
        assertThat(registry.methodRegistry.methodMap).isEmpty()
        assertThat(registry.typeRegistry.messageDescriptors.map { it.name }).containsExactlyInAnyOrder(
            ThirdLevelType.getDescriptor().name,
            NestedTypeOuter.getDescriptor().name,
            EmptyMessage.getDescriptor().name,
            Any.getDescriptor().name,
        )
    }

    @Test
    fun testReflectionTestProto() {
        val registry = Registry.fromProtos(
            listOf(
                ReflectionTestProto.getDescriptor().toProto(),
                ReflectionTestDepthTwoProto.getDescriptor().toProto(),
                ReflectionTestDepthTwoAlternateProto.getDescriptor().toProto(),
                ReflectionTestDepthThreeProto.getDescriptor().toProto(),
                AnyProto.getDescriptor().toProto()
            )
        )
        assertThat(registry.methodRegistry.methodMap.keys.map { it.fullName }).containsExactlyInAnyOrder(
            "${ReflectableServiceGrpc::class.java.packageName}.ReflectableService.Method",
            "${AnotherReflectableServiceGrpc::class.java.packageName}.AnotherReflectableService.Method"
        )
        assertThat(registry.typeRegistry.messageDescriptors.map { it.name }).containsExactlyInAnyOrder(
            ThirdLevelType.getDescriptor().name,
            NestedTypeOuter.getDescriptor().name,
            EmptyMessage.getDescriptor().name,
            Any.getDescriptor().name,
            ThirdLevelTypeSubMsg2.getDescriptor().name,
            Request.getDescriptor().name,
            Reply.getDescriptor().name,
            ThirdLevelTypeSubMsg1.getDescriptor().name
        )
    }

    @Test
    fun testConvertOutput() {
        val request = GreetRequest.newBuilder()
            .setGreeting(Greeting.newBuilder().setName("test").build())
            .build()
        val registry = Registry.fromProtos(
            listOf(
                GreetingProto.getDescriptor().toProto()
            )
        )
        val responses = runBlocking {
            registry.typeRegistry.convertOutput(flowOf(request)).toList()
        }
        assertThat(responses).hasSize(1)
        assertThat(parseGreeting(responses.first())).isEqualTo("test")
    }
}
