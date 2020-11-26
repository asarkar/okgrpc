package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.GrpcMethod
import com.asarkar.okgrpc.ManagedChannelFactory
import com.asarkar.okgrpc.OkGrpcClient
import com.asarkar.okgrpc.exchange
import com.asarkar.okgrpc.findProtoBySymbol
import com.asarkar.okgrpc.listServices
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

internal interface OkGrpcCommandHandler<in T : OkGrpcCommand> {
    fun handleCommand(command: T): List<String>
}

internal class OkGrpcGetCommandHandler : OkGrpcCommandHandler<OkGrpcGetCommand> {
    override fun handleCommand(command: OkGrpcGetCommand): List<String> {
        val client = OkGrpcClient.Builder()
            .withChannel(ManagedChannelFactory.getInstance(command.address))
            .build()
        return runBlocking {
            client.listServices()
                .filter { svc ->
                    command.patterns.isEmpty() ||
                        command.patterns.any { svc.toLowerCase().contains(it.toLowerCase()) }
                }
                .onCompletion { client.close() }
                .toList()
        }
    }
}

internal class OkGrpcDescCommandHandler : OkGrpcCommandHandler<OkGrpcDescCommand> {
    override fun handleCommand(command: OkGrpcDescCommand): List<String> {
        val client = OkGrpcClient.Builder()
            .withChannel(ManagedChannelFactory.getInstance(command.address))
            .build()
        val proto = runBlocking {
            client.use { it.findProtoBySymbol(command.symbol) }
        }
        return when (command.kind) {
            SymbolType.SERVICE -> listOf(proto.toString())
            SymbolType.METHOD ->
                proto.serviceList
                    .flatMap { it.methodList }
                    .filter { it.name == GrpcMethod.parseMethod(command.symbol).method }
                    .map { it.toString() }
            else ->
                proto.messageTypeList
                    .filter { msg -> msg.name == command.symbol.takeLastWhile { it != '.' } }
                    .map { it.toString() }
        }
    }
}

internal class OkGrpcExecCommandHandler : OkGrpcCommandHandler<OkGrpcExecCommand> {
    override fun handleCommand(command: OkGrpcExecCommand): List<String> {
        val client = OkGrpcClient.Builder()
            .withChannel(ManagedChannelFactory.getInstance(command.address))
            .build()
        return runBlocking {
            client.exchange(
                command.method,
                command.arguments.asFlow(),
                headers = command.headers,
                protoPaths = command.protoPaths,
                protoFile = command.protoFile
            )
                .onCompletion { client.close() }
                .toList()
        }
    }
}
