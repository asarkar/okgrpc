package com.asarkar.okgrpc

import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.netty.NettyChannelBuilder

public object ManagedChannelFactory {
    public fun getInstance(address: String): ManagedChannel {
        return if (address.contains(':')) {
            // TODO: Support configuring from property file
            NettyChannelBuilder.forTarget(address)
                .usePlaintext()
                .build()
        } else {
            InProcessChannelBuilder.forName(address)
                .directExecutor()
                .build()
        }
    }
}
