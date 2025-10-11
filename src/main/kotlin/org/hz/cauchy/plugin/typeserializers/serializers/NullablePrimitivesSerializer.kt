package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * 可空基本类型序列化器
 * 处理Byte, Short, Integer, Long, Float, Double, Boolean, Character等包装类型
 */
class NullablePrimitivesSerializer(private val typeName: String) : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeValue(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = ($typeName) $parcel.readValue($typeName.class.getClassLoader());"
    }
}

