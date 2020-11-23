package com.asarkar.okgrpc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class GrpcMethodTest {
    @Test
    fun testParseMethod() {
        val ab = GrpcMethod.parseMethod("a.b")
        assertThat(ab.`package`).isNull()
        assertThat(ab.service).isEqualTo("a")
        assertThat(ab.method).isEqualTo("b")
        assertThat(ab.fullServiceName).isEqualTo("a")
        assertThat(ab.fullName).isEqualTo("a.b")

        val abc = GrpcMethod.parseMethod("a.b.c")
        assertThat(abc.`package`).isEqualTo("a")
        assertThat(abc.service).isEqualTo("b")
        assertThat(abc.method).isEqualTo("c")
        assertThat(abc.fullServiceName).isEqualTo("a.b")
        assertThat(abc.fullName).isEqualTo("a.b.c")
    }

    @Test
    fun testParseMethodInvalid() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { GrpcMethod.parseMethod("") }
            .withMessage("Method name must not be empty")

        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { GrpcMethod.parseMethod(".") }
            .withMessage("Method name must not be empty")

        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { GrpcMethod.parseMethod("a.") }
            .withMessage("Method name must not be empty")

        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { GrpcMethod.parseMethod("a") }
            .withMessage("Service name must not be empty")
    }
}
