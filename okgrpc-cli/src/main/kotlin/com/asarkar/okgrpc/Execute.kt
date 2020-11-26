package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.OkGrpcCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcExecCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file

/**
 * exec subcommand. Executes a RPC method based on gRPC reflection, or local proto files.
 *
 * @author Abhijit Sarkar
 */
internal class Execute(private val handler: OkGrpcCommandHandler<OkGrpcExecCommand>) : CliktCommand(name = "exec") {
    private val method by argument(help = "Fully-qualified method to execute")
    private val cmdArgs: List<String> by argument(help = "Space delimited JSON string arguments").multiple()
    private val fileArgs by option("--file", "-f", help = "File to read arguments from")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.readLines() }
    private val protoPath by option("--proto-path", "-pp", help = "Directory in which to search for imports")
        .file(mustExist = true, canBeDir = true)
        .multiple()
    private val protoFile by option("--proto-file", "-pf", help = "File path relative to the proto directory")
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
        if (protoPath.isEmpty() && protoFile != null) {
            throw UsageError("Proto path must be specified if proto file is")
        }
        if (protoPath.isNotEmpty() && protoFile == null) {
            throw UsageError("Proto file must be specified if proto path is")
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
                        .toMap(),
                    protoPath.map { it.absolutePath },
                    protoFile
                )
            )
                .forEach(::println)
        } catch (ex: Exception) {
            if (config.stacktraceEnabled) throw ex
            else System.err.println(ex.message)
        }
    }
}
