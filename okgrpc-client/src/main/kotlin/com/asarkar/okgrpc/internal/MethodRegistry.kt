package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.GrpcMethod
import com.google.protobuf.Descriptors

internal class MethodRegistry(internal val methodMap: Map<GrpcMethod, Descriptors.MethodDescriptor>) {
    internal fun getMethodDescriptor(method: GrpcMethod): Descriptors.MethodDescriptor = methodMap[method]
        ?: error("Unknown method: $method")
}
