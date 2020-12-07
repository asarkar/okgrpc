package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.OkGrpcCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcDescCommand
import com.asarkar.okgrpc.internal.SymbolType
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch

/**
 * desc subcommand. Fetches detail information about a service, method or type using gRPC reflection.
 *
 * @author Abhijit Sarkar
 */
internal class Describe(private val handler: OkGrpcCommandHandler<OkGrpcDescCommand>) : CliktCommand(name = "desc") {
    private val kind by option(help = "What to describe, service, method, or type").switch(
        "-s" to "SERVICE",
        "--service" to "SERVICE",
        "-m" to "METHOD",
        "--method" to "METHOD",
        "-t" to "TYPE",
        "--type" to "TYPE"
    ).default("SERVICE").convert { SymbolType.valueOf(it) }
    private val symbol by argument(help = "Fully-qualified symbol to describe")
    private val config by requireObject<OkGrpcCli.Config>()

    override fun run() {
        try {
            handler.handleCommand(OkGrpcDescCommand(kind, config.address, symbol))
                .forEach(::println)
        } catch (ex: Exception) {
            if (config.stacktraceEnabled) ex.printStackTrace()
            else System.err.println(ex.message)
        }
    }
}
