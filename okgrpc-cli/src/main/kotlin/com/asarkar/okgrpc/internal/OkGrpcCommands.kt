package com.asarkar.okgrpc.internal

internal interface OkGrpcCommand

internal enum class SymbolType {
    SERVICE, METHOD, TYPE
}

internal data class OkGrpcGetCommand(
    val address: String,
    val patterns: Set<String> = emptySet()
) : OkGrpcCommand

internal data class OkGrpcDescCommand(
    val kind: SymbolType,
    val address: String,
    val symbol: String
) : OkGrpcCommand

internal data class OkGrpcExecCommand(
    val address: String,
    val method: String,
    val arguments: List<String>
) : OkGrpcCommand
