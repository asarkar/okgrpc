package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.Registry
import com.asarkar.okgrpc.internal.call
import com.asarkar.okgrpc.internal.dynamicMessageMarshaller
import com.asarkar.okgrpc.internal.lookupProtos
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import io.grpc.CallOptions
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(OkGrpcClient::class.java)

public fun OkGrpcClient.listServices(): Flow<String> {
    return callbackFlow {
        val requestObserver = ServerReflectionGrpc.newStub(this@listServices.channel)
            .withDeadlineAfter(this@listServices.timeout.inMilliseconds.toLong(), TimeUnit.MILLISECONDS)
            .serverReflectionInfo(
                object : StreamObserver<ServerReflectionResponse> {
                    override fun onNext(response: ServerReflectionResponse) {
                        response.listServicesResponse.serviceList
                            .forEach {
                                logger.debug("Found service: {}", it.name)
                                check(offer(it.name)) { "Failed to deliver: ${it.name}" }
                            }
                    }

                    override fun onError(t: Throwable) {
                        cancel("List error: ${t.message}", t)
                    }

                    override fun onCompleted() {
                        close()
                    }
                }
            )
        requestObserver.onNext(
            ServerReflectionRequest.newBuilder()
                .setListServices("")
                .build()
        )
        requestObserver.onCompleted()
        awaitClose {}
    }
}

public fun OkGrpcClient.listServiceProtos(): Flow<DescriptorProtos.FileDescriptorProto> {
    return listServices()
        .flatMapMerge(Runtime.getRuntime().availableProcessors() / 2, this::lookupProtos)
}

public suspend fun OkGrpcClient.findProtoBySymbol(symbol: String): DescriptorProtos.FileDescriptorProto {
    return lookupProtos(symbol, false)
        .first()
}

@JvmOverloads
public suspend fun OkGrpcClient.exchange(
    method: String,
    requests: Flow<String>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    headers: Map<String, String> = emptyMap()
): Flow<String> {
    val grpcMethod = GrpcMethod.parseMethod(method)
    val metadata = headers.entries.fold(Metadata()) { acc, kv ->
        val key = Metadata.Key.of(kv.key, Metadata.ASCII_STRING_MARSHALLER)
        acc.put(key, kv.value)
        acc
    }
    return exchange(grpcMethod, requests, callOptions, metadata)
}

@JvmOverloads
public suspend fun OkGrpcClient.exchange(
    method: GrpcMethod,
    requests: Flow<String>,
    callOptions: CallOptions = CallOptions.DEFAULT,
    metadata: Metadata = Metadata()
): Flow<String> {
    val protos = lookupProtos(method.fullServiceName)
    val registry = Registry.fromProtos(protos)
    val md = registry.methodRegistry.getMethodDescriptor(method)

    val input = registry.typeRegistry.convertInput(md.inputType, requests)
    val md2 = MethodDescriptor.newBuilder<DynamicMessage, DynamicMessage>()
        .setFullMethodName(MethodDescriptor.generateFullMethodName(md.service.fullName, md.name))
        .setType(md.methodType())
        .setRequestMarshaller(md.inputType.dynamicMessageMarshaller())
        .setResponseMarshaller(md.outputType.dynamicMessageMarshaller())
        .build()

    return call(channel, md2, input, callOptions, metadata)
        .let(registry.typeRegistry::convertOutput)
}

private fun Descriptors.MethodDescriptor.methodType(): MethodDescriptor.MethodType {
    return if (isClientStreaming && isServerStreaming) {
        MethodDescriptor.MethodType.BIDI_STREAMING
    } else if (!this.isClientStreaming && !this.isServerStreaming) {
        MethodDescriptor.MethodType.UNARY
    } else if (isClientStreaming) {
        MethodDescriptor.MethodType.CLIENT_STREAMING
    } else if (isServerStreaming) {
        MethodDescriptor.MethodType.SERVER_STREAMING
    } else MethodDescriptor.MethodType.UNKNOWN
}
