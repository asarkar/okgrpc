package com.asarkar.okgrpc.internal

internal interface OkGrpcCommand

internal data class OkGrpcGetCommand(
    val address: String,
    val patterns: Set<String> = emptySet()
) : OkGrpcCommand

internal enum class DescKind {
    SERVICE, METHOD, TYPE
}

internal data class OkGrpcDescCommand(
    val kind: DescKind,
    val address: String,
    val symbol: String
) : OkGrpcCommand

internal data class OkGrpcExecCommand(
    val address: String,
    val method: String,
    val arguments: List<String>
) : OkGrpcCommand
