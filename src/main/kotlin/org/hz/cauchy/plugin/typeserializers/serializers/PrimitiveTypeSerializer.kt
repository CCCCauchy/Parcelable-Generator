package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * 基本类型序列化器
 * 处理int, long, float, double, boolean, byte, short, char, String等基本类型
 */
class PrimitiveTypeSerializer(private val typeName: String) : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.write$typeName(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = $parcel.read$typeName();"
    }
}

