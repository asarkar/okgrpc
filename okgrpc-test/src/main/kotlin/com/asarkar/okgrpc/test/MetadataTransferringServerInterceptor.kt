package com.asarkar.okgrpc.test

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor

public class MetadataTransferringServerInterceptor : ServerInterceptor {
    public override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val ctx = if (call.methodDescriptor.fullMethodName.endsWith("/Echo")) {
            Context.current().withValue(metadataCtxKey, headers.toJson())
        } else Context.current()

        return Contexts.interceptCall(ctx, call, headers, next)
    }
}
