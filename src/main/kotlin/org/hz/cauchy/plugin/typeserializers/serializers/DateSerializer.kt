package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Date序列化器
 * 特殊处理Date类型
 */
class DateSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeLong(${field.name}.getTime());"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = new java.util.Date($parcel.readLong());"
    }
}


