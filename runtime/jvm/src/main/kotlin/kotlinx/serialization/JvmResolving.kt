/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * TODO doc
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH")
@UseExperimental(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
public inline fun <reified T> serializerFor(): KSerializer<T> {
    return serializerByKType(typeOf<T>()) as KSerializer<T>
}

/**
 * This method uses reflection to construct serializer for given type. However,
 * since it accepts type token, it can work correctly even with generics, so
 * it is not annotated with [ImplicitReflectionSerializer].
 *
 * Keep in mind that this is a 'heavy' call, so result probably should be cached somewhere else.
 *
 * This method intended for static, format-agnostic resolving (e.g. in adapter factories) so context is not used here.
 *
 * @see typeOf
 */
@Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH")
@UseExperimental(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
public fun serializerByKType(type: KType): KSerializer<Any?> {
    fun serializerByKTypeImpl(type: KType): KSerializer<Any> {
        val rootClass = when (val t = type.classifier) {
            is KClass<*> -> t
            is KTypeParameter -> error("KTypeParameter is not supported as classifier, found $t")
            else -> error("Non-denotable type $t")
        } as KClass<Any>
        val args = type.arguments
            .map { requireNotNull(it.type) { "Star projections are not allowed" } }
            .map(::serializerByKType)
        return when {
            type.arguments.isEmpty() -> requireNotNull(rootClass.serializer())
            rootClass.java.isArray -> // How to check this without .java ?
                ReferenceArraySerializer<Any, Any?>(rootClass, args[0])
            else -> when (rootClass) {
                List::class, ArrayList::class -> ArrayListSerializer(args[0])
                HashSet::class -> HashSetSerializer(args[0])
                Set::class, LinkedHashSet::class -> LinkedHashSetSerializer(args[0])
                HashMap::class -> HashMapSerializer(args[0], args[1])
                Map::class, LinkedHashMap::class -> LinkedHashMapSerializer(args[0], args[1])
                Map.Entry::class -> MapEntrySerializer(args[0], args[1])
                Pair::class -> PairSerializer(args[0], args[1])
                Triple::class -> TripleSerializer(args[0], args[1], args[2])
                else ->
                    rootClass.java.invokeSerializerGetter(*args.toTypedArray())
                            ?: requireNotNull(rootClass.serializer())
            }
        } as KSerializer<Any>
    }

    val result = serializerByKTypeImpl(type)
    return if (type.isMarkedNullable) makeNullable(result) else result as KSerializer<Any?>
}
