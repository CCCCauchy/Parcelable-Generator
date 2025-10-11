package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import org.hz.cauchy.plugin.typeserializers.serializers.ParcelableObjectSerializer

/**
 * Parcelable序列化器工厂
 * 检查类型是否实现了Parcelable接口
 */
class ParcelableSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        if (psiType is PsiClassType) {
            val psiClass = psiType.resolve() ?: return null
            if (isImplementParcelable(psiClass)) {
                return ParcelableObjectSerializer()
            }
        }
        return null
    }

    /**
     * 检查类是否实现了Parcelable接口
     */
    private fun isImplementParcelable(psiClass: PsiClass): Boolean {
        val parcelableClass: PsiClass = JavaPsiFacade.getInstance(psiClass.project)
            .findClass("android.os.Parcelable", GlobalSearchScope.allScope(psiClass.project))
            ?: return false

        return psiClass.isInheritor(parcelableClass, true)
    }
}
