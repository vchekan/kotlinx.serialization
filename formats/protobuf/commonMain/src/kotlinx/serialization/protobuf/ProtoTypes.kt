/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

public enum class ProtoNumberType {
    DEFAULT, SIGNED, FIXED
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(val type: ProtoNumberType)

internal typealias ProtoDesc = Pair<Int, ProtoNumberType>

internal fun extractParameters(descriptor: SerialDescriptor, index: Int, zeroBasedDefault: Boolean = false): ProtoDesc {
    if (descriptor.kind is PolymorphicKind) return (if (zeroBasedDefault) index else index + 1) to ProtoNumberType.DEFAULT
    val idx = getProtoId(descriptor, index) ?: (if (zeroBasedDefault) index else index + 1)
    val format = descriptor.findAnnotation<ProtoType>(index)?.type
            ?: ProtoNumberType.DEFAULT
    return idx to format
}


public class ProtobufDecodingException(message: String) : SerializationException(message)

@Suppress("DEPRECATION_ERROR")
internal fun getProtoId(desc: SerialDescriptor, index: Int): Int?
        = desc.findAnnotation<ProtoId>(index)?.id ?: desc.findAnnotation<SerialId>(index)?.id
