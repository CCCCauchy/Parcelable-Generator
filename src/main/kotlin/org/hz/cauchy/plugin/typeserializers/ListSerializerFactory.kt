package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import org.hz.cauchy.plugin.typeserializers.serializers.GenericListSerializer

/**
 * List序列化器工厂
 * 处理各种List类型的序列化
 */
class ListSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        val elementType = PsiUtil.extractIterableTypeParameter(psiType, false)
        return if (elementType != null) {
            GenericListSerializer()
        } else {
            null
        }
    }
}


