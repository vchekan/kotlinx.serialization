/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.*

// this has implicit @Polymorphic
interface IMessage {
    val body: String
}

// and this class too has implicit @Polymorphic
@Serializable
abstract class Message() : IMessage {
    abstract override val body: String
}

@Polymorphic
@Serializable
@SerialName("SimpleMessage") // to cut out package prefix
open class SimpleMessage() : Message() {
    override var body: String = "Simple"
}

@Serializable
@SerialName("DoubleSimpleMessage")
class DoubleSimpleMessage(val body2: String) : SimpleMessage()

@Serializable
@SerialName("MessageWithId")
open class MessageWithId(val id: Int, override val body: String) : Message()

@Serializable
class Holder(
    val iMessage: IMessage,
    val iMessageList: List<IMessage>,
    val message: Message,
    val msgSet: Set<Message>,
    val simple: SimpleMessage,
    // all above should be polymorphic
    val withId: MessageWithId // but this not
)

class PolymorphicOnClassesTest {
    private fun genTestData(): Holder {
        var cnt = -1
        fun gen(): MessageWithId {
            cnt++
            return MessageWithId(cnt, "Message #$cnt")
        }

        return Holder(gen(), listOf(gen(), gen()), gen(), setOf(SimpleMessage()), DoubleSimpleMessage("DoubleSimple"), gen())
    }

    private val testModule = SerializersModule {
        polymorphic(Message::class, IMessage::class, SimpleMessage::class) {
            addSubclass(SimpleMessage::class, SimpleMessage.serializer())
            addSubclass(DoubleSimpleMessage::class, DoubleSimpleMessage.serializer())
            addSubclass(MessageWithId::class, MessageWithId.serializer())
        }
    }

    @Test
    fun testEnablesImplicitlyOnInterfacesAndAbstractClasses() {
        val json = Json(unquoted = true, indented = false, context = testModule)
        val data = genTestData()
        assertEquals("""{iMessage:[MessageWithId,{id:0,body:"Message #0"}],iMessageList:[[MessageWithId,{id:1,body:"Message #1"}],[MessageWithId,{id:2,body:"Message #2"}]],message:[MessageWithId,{id:3,body:"Message #3"}],msgSet:[[SimpleMessage,{body:Simple}]],simple:[DoubleSimpleMessage,{body:Simple,body2:DoubleSimple}],withId:{id:4,body:"Message #4"}}""", json.stringify(Holder.serializer(), data))
    }

    @Test
    fun descriptorsSchemaIsCorrect() {
        val desc = Holder.serializer().descriptor
        assertTrue(desc.getElementDescriptor(0) is PolymorphicClassDescriptor)
        assertEquals("IMessage", desc.getElementDescriptor(0).name)
    }
}
