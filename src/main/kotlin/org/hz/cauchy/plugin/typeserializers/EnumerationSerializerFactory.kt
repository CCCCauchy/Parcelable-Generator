package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.EnumerationSerializer

/**
 * 枚举序列化器工厂
 * 处理枚举类型
 */
class EnumerationSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        if (psiType is PsiClassType) {
            val psiClass = psiType.resolve() ?: return null
            if (psiClass.isEnum) {
                return EnumerationSerializer()
            }
        }
        return null
    }
}


