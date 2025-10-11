package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Parcelable数组序列化器
 * 处理实现了Parcelable接口的对象数组
 */
class ParcelableArraySerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeParcelableArray(${field.name}, $flags);"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        val typeName = field.type.canonicalText
        // 移除数组后缀[]，获取元素类型
        val elementType = typeName.substring(0, typeName.length - 2)
        return "${field.simpleName} = ($typeName) $parcel.readParcelableArray($elementType.class.getClassLoader());"
    }
}
