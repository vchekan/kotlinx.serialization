/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlin.test.*

class MapLikeSerializerTest : JsonTestBase() {

    @Serializable
    data class StringPair(val a: String, val b: String)

    @Serializer(forClass = StringPair::class)
    object StringPairSerializer : MapLikeSerializer<StringPair, String, String> {
        override val keySerializer: KSerializer<String> = String.serializer()
        override val valueSerializer: KSerializer<String> = String.serializer()

        override val descriptor: SerialDescriptor = SerialDescriptor("package.CustomPair", StructureKind.MAP) {
            element<String>("a")
            element<String>("b")
        }

        override fun serialize(encoder: Encoder, value: StringPair) {
            val structuredEncoder = encoder.beginStructure(descriptor)
            structuredEncoder.encodeSerializableElement(descriptor, 0, keySerializer, value.a)
            structuredEncoder.encodeSerializableElement(descriptor, 1, valueSerializer, value.b)
            structuredEncoder.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): StringPair {
            val composite = decoder.beginStructure(descriptor)
            if (composite.decodeSequentially()) {
                val key = composite.decodeSerializableElement(descriptor, 0, keySerializer)
                val value = composite.decodeSerializableElement(descriptor, 1, valueSerializer)
                return StringPair(key, value)
            }

            var key: String? = null
            var value: String? = null
            mainLoop@ while (true) {
                when (val idx = composite.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> {
                        break@mainLoop
                    }
                    0 -> {
                        key = composite.decodeSerializableElement(descriptor, 0, keySerializer)
                    }
                    1 -> {
                        value = composite.decodeSerializableElement(descriptor, 1, valueSerializer)
                    }
                    else -> throw SerializationException("Invalid index: $idx")
                }
            }
            composite.endStructure(descriptor)
            if (key == null) throw SerializationException("Element 'a' is missing")
            if (value == null) throw SerializationException("Element 'b' is missing")
            @Suppress("UNCHECKED_CAST")
            return StringPair(key, value)
        }

        override fun toMap(value: StringPair): Map<String, String> = mapOf(value.a to value.b)
    }

    @Test
    fun testStringPair() = assertJsonFormAndRestored(StringPairSerializer, StringPair("a", "b"), """{"a":"b"}""")
}
