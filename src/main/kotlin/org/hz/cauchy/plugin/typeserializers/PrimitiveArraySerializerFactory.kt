package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.ParcelableArraySerializer
import org.hz.cauchy.plugin.typeserializers.serializers.PrimitiveArraySerializer

/**
 * 基本类型数组序列化器工厂
 * 处理各种基本类型数组
 */
class PrimitiveArraySerializerFactory : TypeSerializerFactory {

    private val arraySerializers = mapOf(
        "int[]" to PrimitiveArraySerializer("Int"),
        "long[]" to PrimitiveArraySerializer("Long"),
        "float[]" to PrimitiveArraySerializer("Float"),
        "double[]" to PrimitiveArraySerializer("Double"),
        "boolean[]" to PrimitiveArraySerializer("Boolean"),
        "byte[]" to PrimitiveArraySerializer("Byte"),
        "char[]" to PrimitiveArraySerializer("Char")
    )

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        val typeName = psiType.canonicalText
        
        // 检查是否是基本类型数组
        arraySerializers[typeName]?.let { return it }
        
        // 检查是否是Parcelable数组
        if (typeName.endsWith("[]")) {
            return ParcelableArraySerializer()
        }
        
        return null
    }
}


