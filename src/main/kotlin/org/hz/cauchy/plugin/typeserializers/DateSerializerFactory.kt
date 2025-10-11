package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.DateSerializer

/**
 * Date序列化器工厂
 * 处理Date类型
 */
class DateSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        return if (psiType.canonicalText == "java.util.Date") {
            DateSerializer()
        } else {
            null
        }
    }
}


