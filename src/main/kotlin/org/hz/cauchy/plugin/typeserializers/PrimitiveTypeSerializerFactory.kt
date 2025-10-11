package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.BooleanPrimitiveSerializer
import org.hz.cauchy.plugin.typeserializers.serializers.CharPrimitiveSerializer
import org.hz.cauchy.plugin.typeserializers.serializers.NullablePrimitivesSerializer
import org.hz.cauchy.plugin.typeserializers.serializers.PrimitiveTypeSerializer
import org.hz.cauchy.plugin.typeserializers.serializers.ShortPrimitiveSerializer

/**
 * 基本类型序列化器工厂
 * 处理所有基本类型和包装类型的序列化
 */
class PrimitiveTypeSerializerFactory : TypeSerializerFactory {

    private val serializers = mapOf(
        // 基本类型
        "byte" to PrimitiveTypeSerializer("Byte"),
        "double" to PrimitiveTypeSerializer("Double"),
        "float" to PrimitiveTypeSerializer("Float"),
        "short" to ShortPrimitiveSerializer(),
        "int" to PrimitiveTypeSerializer("Int"),
        "long" to PrimitiveTypeSerializer("Long"),
        "boolean" to BooleanPrimitiveSerializer(),
        "char" to CharPrimitiveSerializer(),
        "java.lang.String" to PrimitiveTypeSerializer("String"),
        
        // 包装类型
        "java.lang.Byte" to NullablePrimitivesSerializer("java.lang.Byte"),
        "java.lang.Double" to NullablePrimitivesSerializer("java.lang.Double"),
        "java.lang.Float" to NullablePrimitivesSerializer("java.lang.Float"),
        "java.lang.Short" to NullablePrimitivesSerializer("java.lang.Short"),
        "java.lang.Integer" to NullablePrimitivesSerializer("java.lang.Integer"),
        "java.lang.Long" to NullablePrimitivesSerializer("java.lang.Long"),
        "java.lang.Boolean" to NullablePrimitivesSerializer("java.lang.Boolean"),
        "java.lang.Character" to NullablePrimitivesSerializer("java.lang.Character")
    )

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        return serializers[psiType.canonicalText]
    }
}

