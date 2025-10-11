package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.SparseArraySerializer

/**
 * SparseArray序列化器工厂
 * 处理SparseArray类型
 */
class SparseArraySerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        val typeName = psiType.canonicalText
        return if (typeName.startsWith("android.util.SparseArray")) {
            SparseArraySerializer()
        } else {
            null
        }
    }
}


