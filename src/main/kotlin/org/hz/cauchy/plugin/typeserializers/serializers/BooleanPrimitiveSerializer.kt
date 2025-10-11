package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Boolean基本类型序列化器
 * 特殊处理boolean类型，因为Parcel没有直接的boolean方法
 */
class BooleanPrimitiveSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeByte(if (${field.name}) 1 else 0);"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = $parcel.readByte() != 0;"
    }
}

