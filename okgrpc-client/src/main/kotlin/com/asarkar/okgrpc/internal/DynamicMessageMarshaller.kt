package com.asarkar.okgrpc.internal

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.ExtensionRegistryLite
import io.grpc.MethodDescriptor
import java.io.InputStream

internal class DynamicMessageMarshaller internal constructor(
    private val messageDescriptor: Descriptors.Descriptor
) : MethodDescriptor.Marshaller<DynamicMessage> {
    override fun stream(value: DynamicMessage): InputStream {
        return value.toByteString().newInput()
    }

    override fun parse(stream: InputStream): DynamicMessage {
        return DynamicMessage.newBuilder(messageDescriptor)
            .mergeFrom(stream, ExtensionRegistryLite.getEmptyRegistry())
            .build()
    }
}

internal fun Descriptors.Descriptor.dynamicMessageMarshaller() = DynamicMessageMarshaller(this)
