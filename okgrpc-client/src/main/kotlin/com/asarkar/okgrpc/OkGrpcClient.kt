package com.asarkar.okgrpc

import io.grpc.ManagedChannel
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * gRPC Java client based on [gRPC Server Reflection](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md).
 * Can be used to inspect gRPC services and execute RPC methods dynamically without needing a proto file.
 * An instance of the client is tied with a specific [channel].
 *
 * @author Abhijit Sarkar
 */
public class OkGrpcClient private constructor(
    public val channel: ManagedChannel,
    public val timeout: Duration,
    public val shutdownTimeout: Duration
) : AutoCloseable {
    /**
     * OkGrpc client builder.
     *
     * @author Abhijit Sarkar
     */
    public class Builder {
        private var channel: ManagedChannel? = null
        private var timeout: Duration = 1.toDuration(DurationUnit.SECONDS)
        private var shutdownTimeout: Duration = 1.toDuration(DurationUnit.SECONDS)

        public fun withChannel(channel: ManagedChannel): Builder {
            this.channel = channel
            return this
        }

        public fun withTimeout(timeout: Duration): Builder {
            this.timeout = timeout
            return this
        }

        public fun withShutdownTimeout(shutdownTimeout: Duration): Builder {
            this.shutdownTimeout = shutdownTimeout
            return this
        }

        public fun build(): OkGrpcClient {
            require(channel != null) { "Channel must not be null" }
            return OkGrpcClient(channel!!, timeout, shutdownTimeout)
        }
    }

    override fun close() {
        channel.shutdown()
            .awaitTermination(shutdownTimeout.inMilliseconds.toLong(), TimeUnit.MILLISECONDS)
            .also { if (!it) channel.shutdownNow() }
    }
}
