package org.hz.cauchy.plugin.typeserializers.serializers

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * 通用List序列化器
 * 处理各种List类型的序列化
 */
class GenericListSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        val elementType = PsiUtil.extractIterableTypeParameter(field.type, false)
        return when {
            elementType?.canonicalText == "java.lang.String" -> "$parcel.writeStringList(${field.name});"
            isParcelableType(elementType) -> "$parcel.writeTypedList(${field.name});"
            else -> "$parcel.writeList(${field.name});"
        }
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        val elementType = PsiUtil.extractIterableTypeParameter(field.type, false)
        return when {
            elementType?.canonicalText == "java.lang.String" -> "${field.simpleName} = $parcel.createStringArrayList();"
            isParcelableType(elementType) -> {
                val elementTypeName = elementType?.presentableText ?: "Object"
                "${field.simpleName} = $parcel.createTypedArrayList($elementTypeName.CREATOR);"
            }
            else -> {
                val elementTypeName = elementType?.presentableText ?: "Object"
                "${field.simpleName} = new java.util.ArrayList<>();\n" +
                "$parcel.readList(${field.simpleName}, $elementTypeName.class.getClassLoader());"
            }
        }
    }

    private fun isParcelableType(elementType: PsiType?): Boolean {
        if (elementType is PsiClassType) {
            val psiClass = elementType.resolve() ?: return false
            return isImplementParcelable(psiClass)
        }
        return false
    }

    private fun isImplementParcelable(psiClass: com.intellij.psi.PsiClass): Boolean {
        val parcelableClass = com.intellij.psi.JavaPsiFacade.getInstance(psiClass.project)
            .findClass("android.os.Parcelable", com.intellij.psi.search.GlobalSearchScope.allScope(psiClass.project))
            ?: return false
        return psiClass.isInheritor(parcelableClass, true)
    }
}
