package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * 枚举序列化器
 * 处理枚举类型
 */
class EnumerationSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeString(${field.name}.name());"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        val typeName = field.type.canonicalText
        return "${field.simpleName} = $typeName.valueOf($parcel.readString());"
    }
}


