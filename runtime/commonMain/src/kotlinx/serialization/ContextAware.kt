/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.*
import kotlin.reflect.*

public interface ContextAwareDescriptor : SerialDescriptor {
    /**
     * A class which can be used in [SerialModule] as a key
     */
    public val runtimeClass: KClass<*>
}

internal fun SerialDescriptor.withContext(context: KClass<*>): ContextAwareDescriptor =
    object : SerialDescriptor by this, ContextAwareDescriptor {
        override val runtimeClass: KClass<*> = context
    }

public fun SerialModule.getContextualDescriptor(descriptor: ContextAwareDescriptor): SerialDescriptor? =
    getContextual<Any>(descriptor.runtimeClass)?.descriptor


public fun SerialModule.getPolymorphicDescriptors(descriptor: ContextAwareDescriptor): List<SerialDescriptor> {
    // shortcut
    if (this is SerialModuleImpl) return this.polyBase2Serializers[descriptor.runtimeClass]?.values.orEmpty()
        .map { it.descriptor }

    val builder = ArrayList<SerialDescriptor>()
    dumpTo(object : SerialModuleCollector {
        override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) { /*noop*/
        }

        override fun <Base : Any, Sub : Base> polymorphic(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>
        ) {
            if (baseClass == descriptor.runtimeClass) builder.add(actualSerializer.descriptor)
        }
    })
    return builder
}
