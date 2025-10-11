package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.MapSerializer

/**
 * Map序列化器工厂
 * 处理Map类型的序列化
 */
class MapSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        val typeName = psiType.canonicalText
        return if (typeName.startsWith("java.util.Map") || typeName.startsWith("java.util.HashMap")) {
            MapSerializer()
        } else {
            null
        }
    }
}


