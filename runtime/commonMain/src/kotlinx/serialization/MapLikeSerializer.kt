/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

/**
 * Optional interface that represents [serializer][KSerializer] for map-like structures.
 * [T] is a type of target type for the serializer,
 * [K] and [V] type parameters represents types of the key and value.
 *
 * This interface is a marker that is recommended to implement for any serializer of [StructureKind.MAP] kind.
 * Default formats, as well as endorsed ones, recognize serializers of [MapLikeSerializer] type and
 * serialize corresponding values similarly to map.
 *
 * Type [T] does not have restrictions on its upper-bound, for example, it can be a non-[Map] type.
 * For example, it is possible to serialize `StringPair` class as a key-value using [MapLikeSerializer]:
 * ```
 * data class StringPair(val a: String, b: String)
 *
 * @Serializer(StringPair::class)
 * object StringPairSerializer : MapLikeSerializer<StringPair, String, String> {
 *     override val keySerializer: KSerializer<String> = String.serializer()
 *     override val valueSerializer: KSerializer<String> = String.serializer()
 *
 *     override val descriptor: SerialDescriptor = SerialDescriptor("package.StringPair", StructureKind.MAP) {
 *         element<String>("a")
 *         element<String>("b")
 *     }
 *
 *     override fun toMap(value: StringPair): Map<String, String> = mapOf(value.a to value.b)
 *
 *     override fun serialize(encoder: Encoder, value: StringPair) {
 *         val structuredEncoder = encoder.beginStructure(descriptor)
 *         structuredEncoder.encodeSerializableElement(descriptor, 0, keySerializer, value.a)
 *         structuredEncoder.encodeSerializableElement(descriptor, 1, valueSerializer, value.b)
 *         structuredEncoder.endStructure(descriptor)
 *     }
 *     ...
 * }
 * ```
 *
 * With this definition, [Json] format will recognize [MapLikeSerializer] and serialize `StringPair("key", "value")` as `{"key":"value"}`.
 * Similarly, ProtoBuf format will recognize [MapLikeSerializer] and serialize it as map with a single entry.
 */
@InternalSerializationApi
public interface MapLikeSerializer<T, K, V> : KSerializer<T> {

    /**
     * Serializer for the keys of the map representation.
     */
    public val keySerializer: KSerializer<K>

    /**
     * Serializer for the values of the map representation.
     */
    public val valueSerializer: KSerializer<V>

    /**
     * Represents target value [T] as [Map].
     * This method is not guaranteed to be called, for example, JSON
     * makes its decision only based on descriptor [StructureKind.MAP] kind.
     */
    public fun toMap(value: T): Map<K, V>
}
