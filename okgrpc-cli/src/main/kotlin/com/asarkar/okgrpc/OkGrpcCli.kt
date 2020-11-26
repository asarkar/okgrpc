package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.OkGrpcCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcDescCommand
import com.asarkar.okgrpc.internal.OkGrpcDescCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcExecCommand
import com.asarkar.okgrpc.internal.OkGrpcExecCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcGetCommand
import com.asarkar.okgrpc.internal.OkGrpcGetCommandHandler
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

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

public fun main(args: Array<String>) {
    OkGrpcCli
        .newInstance()
        .main(args)
}
