package com.asarkar.okgrpc

import com.asarkar.okgrpc.internal.OkGrpcCommand
import com.asarkar.okgrpc.internal.OkGrpcCommandHandler
import com.asarkar.okgrpc.internal.OkGrpcDescCommand
import com.asarkar.okgrpc.internal.OkGrpcExecCommand
import com.asarkar.okgrpc.internal.OkGrpcGetCommand
import com.asarkar.okgrpc.internal.SymbolType
import com.github.ajalt.clikt.core.MissingArgument
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.NoSuchOption
import com.github.ajalt.clikt.core.UsageError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import java.nio.file.Files
import java.nio.file.Path

@Suppress("UNCHECKED_CAST")
class OkGrpcCliTest {
    private val getCommandHandler = Mockito.mock(OkGrpcCommandHandler::class.java) as OkGrpcCommandHandler<OkGrpcGetCommand>

    private val descCommandHandler = Mockito.mock(OkGrpcCommandHandler::class.java) as OkGrpcCommandHandler<OkGrpcDescCommand>

    private val execCommandHandler = Mockito.mock(OkGrpcCommandHandler::class.java) as OkGrpcCommandHandler<OkGrpcExecCommand>

    private val okGrpcCli = OkGrpcCli.newInstance(getCommandHandler, descCommandHandler, execCommandHandler)

    @AfterEach
    fun afterEach() {
        Mockito.reset(getCommandHandler, descCommandHandler, execCommandHandler)
    }

    @Test
    fun testGet() {
        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "get"))
        var cmd = getLatestCmd(getCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.patterns).isEmpty()

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "get", "-p", "abc", "-p", "def"))
        cmd = getLatestCmd(getCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.patterns).containsExactlyInAnyOrder("abc", "def")

        assertThatExceptionOfType(MissingOption::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("get")) }

        assertThatExceptionOfType(NoSuchOption::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("-a", "localhost:8080", "get", "-s")) }
    }

    @Test
    fun testGetSuppressStacktrace() {
        Mockito.`when`(getCommandHandler.handleCommand(anyNonNull()))
            .thenThrow(RuntimeException("test"))
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("-a", "localhost:8080", "--stacktrace", "get")) }
            .withMessage("test")
        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "get"))
    }

    @Test
    fun testDesc() {
        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "-s", "abc"))
        var cmd = getLatestCmd(descCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.symbol).isEqualTo("abc")
        assertThat(cmd.kind).isEqualTo(SymbolType.SERVICE)

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "-m", "abc"))
        cmd = getLatestCmd(descCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.symbol).isEqualTo("abc")
        assertThat(cmd.kind).isEqualTo(SymbolType.METHOD)

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "-t", "abc"))
        cmd = getLatestCmd(descCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.symbol).isEqualTo("abc")
        assertThat(cmd.kind).isEqualTo(SymbolType.TYPE)

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "abc", "-t"))
        cmd = getLatestCmd(descCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.symbol).isEqualTo("abc")
        assertThat(cmd.kind).isEqualTo(SymbolType.TYPE)

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "abc", "-t"))
        cmd = getLatestCmd(descCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.symbol).isEqualTo("abc")
        assertThat(cmd.kind).isEqualTo(SymbolType.TYPE)

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "abc", "-t", "-m"))
        cmd = getLatestCmd(descCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.symbol).isEqualTo("abc")
        assertThat(cmd.kind).isEqualTo(SymbolType.METHOD)

        assertThatExceptionOfType(MissingArgument::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc")) }
        assertThatExceptionOfType(MissingArgument::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "-m")) }

        assertThatExceptionOfType(NoSuchOption::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("-a", "localhost:8080", "desc", "abc", "-x")) }
    }

    @Test
    fun testExec(@TempDir tempDir: Path) {
        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "exec", "abc", "x", "y"))
        var cmd = getLatestCmd(execCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.method).isEqualTo("abc")
        assertThat(cmd.arguments).containsExactly("x", "y")

        val tempFile = Files.createTempFile(tempDir, null, null).toFile()
        tempFile.writeText("x${System.lineSeparator()}y")

        okGrpcCli.parse(arrayOf("-a", "localhost:8080", "exec", "abc", "-f", tempFile.absolutePath))
        cmd = getLatestCmd(execCommandHandler)
        assertThat(cmd.address).isEqualTo("localhost:8080")
        assertThat(cmd.method).isEqualTo("abc")
        assertThat(cmd.arguments).containsExactly("x", "y")

        assertThatExceptionOfType(UsageError::class.java)
            .isThrownBy { okGrpcCli.parse(arrayOf("-a", "localhost:8080", "exec", "abc")) }
    }

    private inline fun <reified T> anyNonNull(): T = Mockito.any(T::class.java)

    private fun <T : OkGrpcCommand> getLatestCmd(handler: OkGrpcCommandHandler<T>): T {
        return Mockito.mockingDetails(handler).invocations
            .toList()
            .last()
            .getArgument(0)
    }
}
