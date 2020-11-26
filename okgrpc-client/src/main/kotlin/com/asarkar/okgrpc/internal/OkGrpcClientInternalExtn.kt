package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.OkGrpcClient
import com.github.os72.protocjar.Protoc
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
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(OkGrpcClient::class.java)

private object DevNull : OutputStream() {
    override fun write(b: Int) {}
}

internal fun lookupProtos(protoPaths: List<String>, protoFile: String): List<DescriptorProtos.FileDescriptorProto> {
    val tempDir = Files.createTempDirectory("protoc-")
    val resolved = mutableSetOf<String>()
    try {
        return lookupProtos(protoPaths, protoFile, tempDir, resolved)
    } finally {
        tempDir.toFile()
            .walkBottomUp()
            .forEach { it.delete() }
    }
}

private fun lookupProtos(
    protoPaths: List<String>,
    protoFile: String,
    tempDir: Path,
    resolved: MutableSet<String>
): List<DescriptorProtos.FileDescriptorProto> {
    val schema = generateSchema(protoPaths, protoFile, tempDir)
    return schema.fileList
        .filter { resolved.add(it.name) }
        .flatMap { fd ->
            logger.debug("Resolved: {}", fd.name)
            fd.dependencyList
                .filterNot(resolved::contains)
                .flatMap { lookupProtos(protoPaths, it, tempDir, resolved) } + fd
        }
}

/*
 * Generates proto schema using a runtime proto parser.
 * https://github.com/os72/protoc-jar
 * --descriptor_set_out=<file> option writes out a binary file descriptor (compiled schema), that can then be
 * read by FileDescriptorSet.parseFrom.
 */
private fun generateSchema(
    protoPaths: List<String>,
    protoFile: String,
    tempDir: Path
): DescriptorProtos.FileDescriptorSet {
    logger.debug("Generating schema for: {}", protoFile)
    val outFile = Files.createTempFile(tempDir, null, null)
    val stderr = ByteArrayOutputStream()
    val exitCode = Protoc.runProtoc(
        (protoPaths.map { "--proto_path=$it" } + listOf("--descriptor_set_out=$outFile", protoFile)).toTypedArray(),
        DevNull,
        stderr
    )
    if (exitCode != 0) {
        logger.error(stderr.toString("UTF-8"))
        throw IllegalStateException("Failed to generate schema for: $protoFile")
    }
    return Files.newInputStream(outFile).use(DescriptorProtos.FileDescriptorSet::parseFrom)
}

/*
 * Looks up proto files recursively, unless recursive=false.
 */
internal fun OkGrpcClient.lookupProtos(
    symbol: String,
    recursive: Boolean = true
): Flow<DescriptorProtos.FileDescriptorProto> {
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
                            .map(DescriptorProtos.FileDescriptorProto::parseFrom)
                        descriptors
                            .filterNot { requestedDescriptors.contains(it.name) }
                            .forEach { fd ->
                                logger.debug("Received: {}", fd.name)
                                check(offer(fd)) { "Failed to deliver: ${fd.name}" }
                                if (recursive) {
                                    fd.dependencyList
                                        .filterNot(requestedDescriptors::contains)
                                        .forEach(::makeRequest)
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
