package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType

/**
 * 链式序列化器工厂
 * 按顺序尝试多个工厂，直到找到合适的序列化器
 */
class ChainSerializerFactory(
    private val factories: List<TypeSerializerFactory>
) : TypeSerializerFactory {

    constructor(vararg factories: TypeSerializerFactory) : this(factories.toList())

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        for (factory in factories) {
            val serializer = factory.getSerializer(psiType)
            if (serializer != null) {
                return serializer
            }
        }
        return null
    }

    /**
     * 扩展工厂链，添加新的工厂
     */
    fun extend(newFactory: TypeSerializerFactory): ChainSerializerFactory {
        return ChainSerializerFactory(factories + newFactory)
    }
}

