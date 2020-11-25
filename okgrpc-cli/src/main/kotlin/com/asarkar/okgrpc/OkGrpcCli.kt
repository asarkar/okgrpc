package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.OkGrpcCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcDescCommand
import com.asarkar.okgrpc.internal.OkGrpcDescCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcExecCommand
import com.asarkar.okgrpc.internal.OkGrpcExecCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcGetCommand
import com.asarkar.okgrpc.internal.OkGrpcGetCommandHandler
import com.asarkar.okgrpc.internal.SymbolType
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.file

/**
 * gRPC Java CLI based on [gRPC Server Reflection](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md).
 * Can be used to inspect gRPC services and execute RPC methods dynamically without needing a proto file.
 *
 * Type `okgrpc-cli --help` for usage, and `okgrpc-cli <command> --help` for specific command usage.
 *
 * @author Abhijit Sarkar
 */
public class OkGrpcCli private constructor() : CliktCommand(name = "okgrpc-cli") {
    /**
     * OkGRPC CLI configuration.
     *
     * @author Abhijit Sarkar
     */
    public data class Config(
        val stacktraceEnabled: Boolean,
        val address: String
    )

    private val stacktrace by option("--stacktrace", help = "Flag to print stacktrace").flag()
    private val address by option("-a", "--address", help = "gRPC server address in the form host:port")
        .required()

    override fun run() {
        currentContext.findOrSetObject { Config(stacktrace, address) }
    }

    internal companion object {
        /**
         * Creates an instance of [OkGrpcCli]. Intended to be used from unit tests.
         */
        internal fun newInstance(
            getCommandHandler: OkGrpcCommandHandler<OkGrpcGetCommand>,
            descCommandHandler: OkGrpcCommandHandler<OkGrpcDescCommand>,
            execCommandHandler: OkGrpcCommandHandler<OkGrpcExecCommand>
        ): OkGrpcCli {
            return OkGrpcCli().subcommands(
                Get(getCommandHandler),
                Describe(descCommandHandler),
                Execute(execCommandHandler)
            )
        }

        /**
         * Creates an instance of [OkGrpcCli].
         */
        internal fun newInstance(): OkGrpcCli = newInstance(
            OkGrpcGetCommandHandler(),
            OkGrpcDescCommandHandler(),
            OkGrpcExecCommandHandler()
        )
    }
}

private class Get(val handler: OkGrpcCommandHandler<OkGrpcGetCommand>) : CliktCommand() {
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
            if (config.stacktraceEnabled) throw ex
            else System.err.println(ex.message)
        }
    }
}

private class Describe(val handler: OkGrpcCommandHandler<OkGrpcDescCommand>) : CliktCommand(name = "desc") {
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
            if (config.stacktraceEnabled) throw ex
            else System.err.println(ex.message)
        }
    }
}

private class Execute(val handler: OkGrpcCommandHandler<OkGrpcExecCommand>) : CliktCommand(name = "exec") {
    private val method by argument(help = "Fully-qualified method to execute")
    private val cmdArgs: List<String> by argument(help = "Space delimited JSON string arguments").multiple()
    private val fileArgs by option("--file", "-f", help = "File to read arguments from")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.readLines() }
    private val headers: List<String> by option(
        "--header",
        "-h",
        help = "Header in the form of key:value"
    )
        .multiple()
        .check("Header must be in the form of key:value") { headers -> headers.all { it.contains(':') } }
    private val config by requireObject<OkGrpcCli.Config>()

    override fun run() {
        if (cmdArgs.isEmpty() && (fileArgs == null || fileArgs!!.isEmpty())) {
            throw UsageError("Either arguments or a file path must be provided")
        }
        try {
            handler.handleCommand(
                OkGrpcExecCommand(
                    config.address,
                    method,
                    cmdArgs.takeIf { it.isNotEmpty() } ?: fileArgs!!,
                    headers.map {
                        val parts = it.split(':')
                        parts.first().trim() to parts.last().trim()
                    }
                        .toMap()
                )
            )
                .forEach(::println)
        } catch (ex: Exception) {
            if (config.stacktraceEnabled) throw ex
            else System.err.println(ex.message)
        }
    }
}

public fun main(args: Array<String>) {
    OkGrpcCli
        .newInstance()
        .main(args)
}
