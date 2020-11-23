package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.OkGrpcClient
import com.google.protobuf.DescriptorProtos
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING
import io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING
import io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING
import io.grpc.MethodDescriptor.MethodType.UNARY
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(OkGrpcClient::class.java)

internal fun OkGrpcClient.lookupProtos(symbol: String, recursive: Boolean = true): Flow<DescriptorProtos.FileDescriptorProto> {
    return callbackFlow {
        var pendingRequests = 0
        val requestedDescriptors = mutableSetOf<String>()
        var requestObserver: StreamObserver<ServerReflectionRequest>? = null

        fun makeRequest(symbol: String) {
            check(requestObserver != null) { "Request observer must not be null" }
            requestedDescriptors.add(symbol)
            if (symbol.endsWith(".proto")) logger.debug("Requesting: {}", symbol)
            else logger.debug("Looking up: {}", symbol)
            pendingRequests += 1
            requestObserver!!.onNext(
                ServerReflectionRequest.newBuilder()
                    .setFileContainingSymbol(symbol)
                    .build()
            )
        }

        requestObserver = ServerReflectionGrpc.newStub(this@lookupProtos.channel)
            .withDeadlineAfter(this@lookupProtos.timeout.inMilliseconds.toLong(), TimeUnit.MILLISECONDS)
            .serverReflectionInfo(
                object : StreamObserver<ServerReflectionResponse> {
                    override fun onNext(response: ServerReflectionResponse) {
                        val descriptors = response.fileDescriptorResponse.fileDescriptorProtoList
                            .map { DescriptorProtos.FileDescriptorProto.parseFrom(it) }
                        descriptors
                            .filterNot { requestedDescriptors.contains(it.name) }
                            .forEach { fd ->
                                logger.debug("Received: {}", fd.name)
                                check(offer(fd)) { "Failed to deliver: ${fd.name}" }
                                if (recursive) {
                                    fd.dependencyList
                                        .filterNot { requestedDescriptors.contains(it) }
                                        .forEach { makeRequest(it) }
                                }
                                pendingRequests -= 1
                                if (pendingRequests == 0) {
                                    requestObserver!!.onCompleted()
                                }
                            }
                    }

                    override fun onError(t: Throwable) {
                        cancel("Lookup error: ${t.message}", t)
                    }

                    override fun onCompleted() {
                        close()
                    }
                }
            )
        makeRequest(symbol)
        awaitClose {}
    }
}

internal suspend fun <I, O> call(
    channel: ManagedChannel,
    method: MethodDescriptor<I, O>,
    requests: Flow<I>,
    callOptions: CallOptions,
    metadata: Metadata
): Flow<O> {
    logger.debug("Channel authority: {}", channel.authority())
    logger.debug("Method name: {}, type: {}", method.fullMethodName, method.type)
    logger.debug("Metadata: {}", metadata)

    return when (method.type) {
        BIDI_STREAMING -> io.grpc.kotlin.ClientCalls.bidiStreamingRpc(
            channel,
            method,
            requests,
            callOptions,
            metadata
        )
        CLIENT_STREAMING -> flow {
            emit(
                io.grpc.kotlin.ClientCalls.clientStreamingRpc(
                    channel,
                    method,
                    requests,
                    callOptions,
                    metadata
                )
            )
        }
        SERVER_STREAMING -> io.grpc.kotlin.ClientCalls.serverStreamingRpc(
            channel,
            method,
            requests.first(),
            callOptions,
            metadata
        )
        UNARY -> flow {
            emit(
                io.grpc.kotlin.ClientCalls.unaryRpc(
                    channel,
                    method,
                    requests.first(),
                    callOptions,
                    metadata
                )
            )
        }
        else -> error("Unknown method type: ${method.type}")
    }
}
