package com.asarkar.okgrpc.internal

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class TypeRegistry(internal val messageDescriptors: List<Descriptors.Descriptor>) {
    private val typeRegistry = com.google.protobuf.TypeRegistry.newBuilder().add(messageDescriptors).build()
    private val parser: JsonFormat.Parser = JsonFormat.parser()
        .usingTypeRegistry(typeRegistry)
    private val printer: JsonFormat.Printer = JsonFormat.printer()
        .usingTypeRegistry(typeRegistry)
        .includingDefaultValueFields()

    internal fun convertInput(inputType: Descriptors.Descriptor, requests: Flow<String>): Flow<DynamicMessage> {
        return requests
            .map { json ->
                DynamicMessage.newBuilder(inputType)
                    .also { parser.merge(json, it) }
                    .build()
            }
    }

    internal fun convertOutput(messages: Flow<MessageOrBuilder>): Flow<String> {
        return messages.map(printer::print)
    }
}
