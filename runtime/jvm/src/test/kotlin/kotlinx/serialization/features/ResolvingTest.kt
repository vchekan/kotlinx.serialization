/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.IntDescriptor
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ResolvingTest {
    @Serializable
    data class Box<out T>(val a: T)

    @Serializable
    data class Data(val l: List<String>, val b: Box<Int>)

    @Serializable
    data class WithCustomDefault(val n: Int) {
        @Serializer(forClass = WithCustomDefault::class)
        companion object {
            override val descriptor: SerialDescriptor = IntDescriptor.withName("WithCustomDefault")
            override fun serialize(encoder: Encoder, obj: WithCustomDefault) = encoder.encodeInt(obj.n)
            override fun deserialize(decoder: Decoder) = WithCustomDefault(decoder.decodeInt())
        }
    }

    @Test
    fun primitiveDescriptorWithNameTest() {
        val desc = WithCustomDefault.serializer().descriptor
        assertEquals("WithCustomDefault", desc.name)
        assertSame(PrimitiveKind.INT, desc.kind)
        assertEquals(0, desc.elementsCount)
    }

    private inline fun <reified T> assertSerializedWithType(
        expected: String,
        obj: T,
        json: StringFormat = Json.unquoted
    ) {
        val serial = serializerFor<T>()
        assertEquals(expected, json.stringify(serial, obj))
    }

    @Test
    fun intBoxTest() {
        val b = Box(42)
        assertSerializedWithType("{a:42}", b)
    }

    @Test
    fun testArrayResolving() {
        val myArr = arrayOf("a", "b", "c")
        assertSerializedWithType("[a,b,c]", myArr)
    }

    @Test
    fun testListResolving() {
        val myArr = listOf("a", "b", "c")
        assertSerializedWithType("[a,b,c]", myArr)
    }


    @Test
    fun testReifiedArrayResolving() {
        val myArr = arrayOf("a", "b", "c")
        assertSerializedWithType("[a,b,c]", myArr)
    }

    @Test
    fun testReifiedSetResolving() {
        val mySet = setOf("a", "b", "c", "c")
        assertSerializedWithType("[a,b,c]", mySet)
    }

    @Test
    fun testReifiedMapResolving() {
        val myMap = mapOf("a" to Data(listOf("c"), Box(6)))
        assertSerializedWithType("{a:{l:[c],b:{a:6}}}", myMap)
    }

    @Test
    fun testNestedListResolving() {
        val myList = listOf(listOf(listOf(1, 2, 3)), listOf())
        assertSerializedWithType("[[[1,2,3]],[]]", myList)
    }

    @Test
    fun equalityOfListAndArrayList() {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<ArrayList<Int>>("[1,2,3]", myList)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
    }

    @Test
    fun equalityOfProjectedLists() {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
        assertSerializedWithType<MutableList<out Int>>("[1,2,3]", myList)
    }

    @Test
    fun supportNullableTypes() {
        val myList: List<Int?> = listOf(1, null, 3)
        assertSerializedWithType("[1,null,3]", myList)
        assertSerializedWithType<List<Int?>?>("[1,null,3]", myList)
    }

    @Test
    fun testNestedArrayResolving() {
        val myList = arrayOf(arrayOf(arrayOf(1, 2, 3)), arrayOf())
        assertSerializedWithType("[[[1,2,3]],[]]", myList)
    }

    @Test
    fun testNestedMixedResolving() {
        val myList = arrayOf(listOf(arrayOf(1, 2, 3)), listOf())
        assertSerializedWithType("[[[1,2,3]],[]]", myList)
    }

    @Test
    fun objectTest() {
        val b = Data(listOf("a", "b", "c"), Box(42))
        assertSerializedWithType("{l:[a,b,c],b:{a:42}}", b)
    }

    @Test
    fun customDefault() {
        val foo = Json.unquoted.parse<WithCustomDefault>("9")
        assertEquals(9, foo.n)
    }

    @Serializable
    data class WithNamedCompanion(val a: Int) {
        companion object Named
    }

    @Test
    fun namedCompanionTest() {
        val namedCompanion = WithNamedCompanion(1)
        assertSerializedWithType("{a:1}", namedCompanion)
    }

    @Test
    fun intResolve() {
        val token = typeOf<Int>()
        val serial = serializerByKType(token)
        assertSame(IntSerializer as KSerializer<*>, serial)
        assertSerializedWithType("42", 42)
    }
}
