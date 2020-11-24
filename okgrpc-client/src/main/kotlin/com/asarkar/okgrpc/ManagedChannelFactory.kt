package com.asarkar.okgrpc

import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import org.slf4j.LoggerFactory

/**
 * A factory to create [ManagedChannel](https://grpc.github.io/grpc-java/javadoc/io/grpc/ManagedChannel.html) instances
 * based on the given address.
 * If the address is in the form host:port, a
 * [NettyChannelBuilder](https://grpc.github.io/grpc-java/javadoc/io/grpc/netty/NettyChannelBuilder.html) is used to
 * create the channel.
 * Otherwise, an in-process channel is created using the address as the name.
 *
 * To avoid conflict with other Netty versions, tries to use
 * [grpc-netty-shaded](https://search.maven.org/artifact/io.grpc/grpc-netty-shaded) if present on the classpath,
 * otherwise uses [grpc-netty](https://search.maven.org/artifact/io.grpc/grpc-netty).
 *
 * @author Abhijit Sarkar
 */
public object ManagedChannelFactory {
    private val logger = LoggerFactory.getLogger(ManagedChannelFactory::class.java)

    /**
     * Returns a [ManagedChannel] constructed based on the given [address].
     */
    public fun getInstance(address: String): ManagedChannel {
        return if (address.contains(':')) {
            try {
                newChannel(address, "io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder")
            } catch (ex: ReflectiveOperationException) {
                newChannel(address, "io.grpc.netty.NettyChannelBuilder")
            }
        } else {
            InProcessChannelBuilder.forName(address)
                .directExecutor()
                .build()
        }
    }

    private fun newChannel(address: String, channelBuilderClassname: String): ManagedChannel {
        val channelBuilderClass = Class.forName(channelBuilderClassname)
        val forTargetMethod = channelBuilderClass.getDeclaredMethod("forTarget", String::class.java)
        var nettyChannelBuilder = forTargetMethod.invoke(null, address)
        val usePlaintextMethod = nettyChannelBuilder.javaClass.getDeclaredMethod("usePlaintext")
        nettyChannelBuilder = usePlaintextMethod.invoke(nettyChannelBuilder)
        val buildMethod = nettyChannelBuilder.javaClass.getMethod("build")
        val channel = buildMethod.invoke(nettyChannelBuilder)

        logger.debug("Created channel from class: {}", channelBuilderClassname)
        return channel as ManagedChannel
    }
}
