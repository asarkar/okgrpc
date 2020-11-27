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
import java.io.File
import java.nio.file.Paths

/**
 * exec subcommand. Executes a RPC method based on gRPC reflection, or local proto files.
 *
 * If local proto files are used, proto schemas are generated using a runtime proto parser. If [protoPath] is
 * specified but a [protoFile] isn't, [protoPath] is deduced as the parent directory of the [protoFile] if it's
 * an absolute path, or the present working directory otherwise. When both are specified, [protoPath] must be a
 * proper prefix of the [protoFile] absolute path.
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
        validate()

        val (pp, pf) = if (protoPath.isEmpty() && protoFile != null) {
            val pf = File(protoFile!!)
            val pair = if (pf.isAbsolute) {
                pf.parentFile to pf.name
            } else {
                Paths.get(".").toAbsolutePath().normalize().toFile() to protoFile!!
            }
            println("Using proto path: ${pair.first}, proto file: ${pair.second}")
            listOf(pair.first) to pair.second
        } else protoPath to protoFile

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
                    pp.map { it.absolutePath },
                    pf
                )
            )
                .forEach(::println)
        } catch (ex: Exception) {
            if (config.stacktraceEnabled) throw ex
            else System.err.println(ex.message)
        }
    }

    private fun validate() {
        if (cmdArgs.isEmpty() && (fileArgs == null || fileArgs!!.isEmpty())) {
            throw UsageError("Either arguments or a file path must be provided")
        }
        if (protoPath.isNotEmpty()) {
            if (protoFile == null) {
                throw UsageError("Proto file must be specified if proto path is")
            } else if (protoPath.none { it.resolve(protoFile!!).exists() }) {
                throw UsageError("Proto path must be a proper prefix of the proto file absolute path")
            }
        }
    }
}
