package com.asarkar.okgrpc

public data class GrpcMethod(
    val `package`: String?,
    val service: String,
    val method: String
) {
    init {
        require(service.isNotEmpty()) { "Service name must not be empty" }
        require(method.isNotEmpty()) { "Method name must not be empty" }
    }

    val fullServiceName: String = listOfNotNull(`package`, service).joinToString(".")

    val fullName: String = "$fullServiceName.$method"

    public companion object {
        public fun parseMethod(methodStr: String): GrpcMethod {
            val i = methodStr.lastIndexOf('.')
            require(i < methodStr.length - 1) { "Method name must not be empty" }
            require(i >= 1) { "Service name must not be empty" }
            val j = methodStr.lastIndexOf('.', i - 1)

            return if (j == -1) GrpcMethod(null, methodStr.substring(0, i), methodStr.substring(i + 1))
            else GrpcMethod(methodStr.substring(0, j), methodStr.substring(j + 1, i), methodStr.substring(i + 1))
        }
    }
}
