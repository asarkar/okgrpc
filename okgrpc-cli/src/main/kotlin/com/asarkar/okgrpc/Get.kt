package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.OkGrpcCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcGetCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique

/**
 * get subcommand. Lists all services using gRPC reflection.
 *
 * @author Abhijit Sarkar
 */
internal class Get(private val handler: OkGrpcCommandHandler<OkGrpcGetCommand>) : CliktCommand() {
    private val patterns: Set<String> by option(
        "--pattern",
        "-p",
        help = "Pattern to filter the services returned; works like case-insensitive grep, no regex"
    )
        .multiple()
        .unique()
    private val config by requireObject<OkGrpcCli.Config>()

    override fun run() {
        try {
            handler.handleCommand(OkGrpcGetCommand(config.address, patterns))
                .forEach(::println)
        } catch (ex: Exception) {
            if (config.stacktraceEnabled) ex.printStackTrace()
            else System.err.println(ex.message)
        }
    }
}
