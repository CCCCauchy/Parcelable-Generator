package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Short基本类型序列化器
 * 特殊处理short类型，因为Parcel没有直接的short方法
 */
class ShortPrimitiveSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeInt(${field.name}.toInt());"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = $parcel.readInt().toShort();"
    }
}

