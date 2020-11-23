package com.asarkar.okgrpc.internal

import com.asarkar.okgrpc.GrpcMethod
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

internal class Registry private constructor(protos: Flow<DescriptorProtos.FileDescriptorProto>) {
    internal val methodRegistry: MethodRegistry
    internal val typeRegistry: TypeRegistry

    init {
        val index: Map<String, DescriptorProtos.FileDescriptorProto> = runBlocking {
            protos
                .map { it.name to it }
                .toList()
                .toMap()
        }
        val cache = mutableMapOf<String, Descriptors.FileDescriptor>()
        val methodMap = mutableMapOf<GrpcMethod, Descriptors.MethodDescriptor>()
        val messageDescriptors = mutableListOf<Descriptors.Descriptor>()
        index.values
            .map { it.resolve(index, cache) }
            .forEach { fd ->
                fd.services
                    .forEach { svc ->
                        svc.methods.forEach { methodMap[GrpcMethod(fd.`package`, svc.name, it.name)] = it }
                    }
                messageDescriptors.addAll(fd.messageTypes)
            }

        this.methodRegistry = MethodRegistry(methodMap)
        this.typeRegistry = TypeRegistry(messageDescriptors)
    }

    private fun DescriptorProtos.FileDescriptorProto.resolve(
        index: Map<String, DescriptorProtos.FileDescriptorProto>,
        cache: MutableMap<String, Descriptors.FileDescriptor>
    ): Descriptors.FileDescriptor {
        if (cache.containsKey(this.name)) return cache[this.name]!!

        return this.dependencyList
            .map { (index[it] ?: error("Unknown dependency: $it")).resolve(index, cache) }
            .let {
                val fd = Descriptors.FileDescriptor.buildFrom(this, *it.toTypedArray())
                cache[fd.name] = fd
                fd
            }
    }

    companion object {
        internal fun fromProtos(protos: Flow<DescriptorProtos.FileDescriptorProto>): Registry = Registry(protos)

        internal fun fromProtos(protos: List<DescriptorProtos.FileDescriptorProto>): Registry = fromProtos(protos.asFlow())
    }
}
