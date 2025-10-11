package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import org.hz.cauchy.plugin.typeserializers.serializers.SerializableObjectSerializer

/**
 * Serializable序列化器工厂
 * 检查类型是否实现了Serializable接口
 */
class SerializableSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        if (psiType is PsiClassType) {
            val psiClass = psiType.resolve() ?: return null
            if (isImplementSerializable(psiClass)) {
                return SerializableObjectSerializer()
            }
        }
        return null
    }

    /**
     * 检查类是否实现了Serializable接口
     */
    private fun isImplementSerializable(psiClass: PsiClass): Boolean {
        val serializableClass: PsiClass = JavaPsiFacade.getInstance(psiClass.project)
            .findClass("java.io.Serializable", GlobalSearchScope.allScope(psiClass.project))
            ?: return false

        return psiClass.isInheritor(serializableClass, true)
    }
}


