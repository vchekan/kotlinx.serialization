/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerFor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleTestsJVM {
    @Test
    fun testHello() {
        assertTrue("JVM" in hello())
    }

    @Test
    fun testNestedMixedResolving() {
        val myList = arrayOf(listOf(arrayOf(1, 2, 3)), listOf())
        val serial = serializerFor<Array<List<Array<Int>>>>()
        assertEquals("[[[1,2,3]],[]]", Json.unquoted.stringify(serial, myList))
    }
}
