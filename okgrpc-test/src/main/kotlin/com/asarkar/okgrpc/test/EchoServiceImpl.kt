package com.asarkar.okgrpc.test

import com.fasterxml.jackson.module.kotlin.readValue
import io.grpc.Context
import io.grpc.stub.StreamObserver

public class EchoServiceImpl : EchoServiceGrpc.EchoServiceImplBase() {
    override fun echo(ping: Ping, responseObserver: StreamObserver<Pong>) {
        responseObserver.onNext(
            Pong.newBuilder()
                .setMessage(ping.message)
                .also { builder ->
                    val headers = metadataCtxKey[Context.current()]?.let {
                        mapper.readValue<Map<String, String>>(it)
                    }
                    val pairs = headers?.entries?.map { header ->
                        Pair.newBuilder().setKey(header.key).setValue(header.value).build()
                    }
                    if (pairs != null && pairs.isNotEmpty()) builder.addAllHeaders(pairs)
                }
                .build()
        )
        responseObserver.onCompleted()
    }
}
