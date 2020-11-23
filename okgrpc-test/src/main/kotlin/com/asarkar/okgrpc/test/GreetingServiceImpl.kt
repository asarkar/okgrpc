package com.asarkar.okgrpc.test

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

public class GreetingServiceImpl : GreetingServiceGrpc.GreetingServiceImplBase() {
    private val logger = LoggerFactory.getLogger(GreetingServiceImpl::class.java)

    override fun greet(request: GreetRequest, responseObserver: StreamObserver<GreetResponse>) {
        if (request.greeting.name.isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("You can do better!")
                    .asRuntimeException()
            )
        } else {
            val response = GreetResponse.newBuilder()
                .setResult("Hello, ${request.greeting.name}")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun greetManyTimes(request: GreetRequest, responseObserver: StreamObserver<GreetResponse>) {
        (1..3)
            .forEach { i ->
                val response = GreetResponse.newBuilder()
                    .setResult("Hello, ${request.greeting.name}[$i]")
                    .build()
                responseObserver.onNext(response)
            }
        responseObserver.onCompleted()
    }

    override fun longGreet(responseObserver: StreamObserver<GreetResponse>): StreamObserver<GreetRequest> {
        return object : StreamObserver<GreetRequest> {
            val result = mutableListOf<String>()
            override fun onError(t: Throwable) {
                logger.error(t.message, t)
            }

            override fun onCompleted() {
                responseObserver
                    .onNext(
                        GreetResponse.newBuilder()
                            .setResult(result.joinToString(separator = "! "))
                            .build()
                    )
                responseObserver.onCompleted()
            }

            override fun onNext(request: GreetRequest) {
                result.add("Hello, ${request.greeting.name}")
            }
        }
    }

    override fun greetEveryone(responseObserver: StreamObserver<GreetResponse>): StreamObserver<GreetRequest> {
        return object : StreamObserver<GreetRequest> {
            override fun onError(t: Throwable) {
                logger.error(t.message, t)
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }

            override fun onNext(request: GreetRequest) {
                responseObserver
                    .onNext(
                        GreetResponse.newBuilder()
                            .setResult("Hello, ${request.greeting.name}")
                            .build()
                    )
            }
        }
    }
}
