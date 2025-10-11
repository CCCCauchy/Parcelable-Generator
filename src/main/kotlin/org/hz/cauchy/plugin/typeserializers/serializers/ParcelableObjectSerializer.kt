package org.hz.cauchy.plugin.typeserializers.serializers

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Parcelable对象序列化器
 * 处理实现了Parcelable接口的对象
 */
class ParcelableObjectSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeParcelable(${field.name}, $flags);"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        val typeName = field.type.canonicalText
        return "${field.simpleName} = $parcel.readParcelable($typeName.class.getClassLoader());"
    }
}

