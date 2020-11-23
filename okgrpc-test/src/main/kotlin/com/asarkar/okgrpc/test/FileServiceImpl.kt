package com.asarkar.okgrpc.test

import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

public class FileServiceImpl : FileServiceGrpc.FileServiceImplBase() {
    private val logger = LoggerFactory.getLogger(FileServiceImpl::class.java)
    private val fileStore = mutableMapOf<String, MutableList<ByteString>>()

    override fun upload(responseObserver: StreamObserver<FileId>): StreamObserver<FileChunk> {
        return object : StreamObserver<FileChunk> {
            private lateinit var id: String
            override fun onError(t: Throwable) {
                logger.error("Upload failure", t)
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }

            override fun onNext(chunk: FileChunk) {
                id = chunk.fileId
                fileStore.compute(chunk.fileId) { _: String, v: MutableList<ByteString>? ->
                    (v ?: mutableListOf()).apply { add(chunk.content) }
                }
                if (chunk.last) responseObserver.onNext(FileId.newBuilder().setId(id).build())
            }
        }
    }

    override fun download(request: FileId, responseObserver: StreamObserver<FileChunk>) {
        if (!fileStore.containsKey(request.id)) responseObserver
            .onError(
                Status.NOT_FOUND
                    .withDescription("No such file with id: ${request.id}$")
                    .asRuntimeException()
            )
        else {
            val chunks = fileStore[request.id]!!
            chunks.zip(1..chunks.size)
                .map { FileChunk.newBuilder().setFileId(request.id).setContent(it.first).setLast(it.second == chunks.size).build() }
                .forEach(responseObserver::onNext)
            responseObserver.onCompleted()
        }
    }
}
