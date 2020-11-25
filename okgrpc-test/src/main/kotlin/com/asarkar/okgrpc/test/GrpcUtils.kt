package com.asarkar.okgrpc.test

import io.grpc.BindableService
import io.grpc.Context
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.inprocess.InProcessServerBuilder
import java.net.ServerSocket
import kotlin.random.Random

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

public val metadataCtxKey: Context.Key<String> = Context.key("metadata")

public fun randomStr(): String {
    return generateSequence {
        val i = Random.nextInt(0, charPool.size)
        charPool[i]
    }
        .take(6)
        .joinToString("")
}

public fun inProcessServer(name: String, vararg services: BindableService): Server {
    return inProcessServer(name, emptyList(), *services)
}

public fun inProcessServer(
    name: String,
    interceptors: List<ServerInterceptor>,
    vararg services: BindableService
): Server {
    return InProcessServerBuilder.forName(name)
        .directExecutor()
        .apply {
            services.forEach { addService(it) }
            interceptors.forEach { intercept(it) }
        }
        .build()
}

public fun randomFreePort(): Int = ServerSocket(0).use { it.localPort }

public fun gRPCServer(port: Int, vararg services: BindableService): Server {
    return ServerBuilder
        .forPort(port)
        .also { services.fold(it) { a, b -> a.addService(b) } }
        .build()
}
