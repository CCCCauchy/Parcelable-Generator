package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Serializable对象序列化器
 * 处理实现了Serializable接口的对象
 */
class SerializableObjectSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeValue(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        val typeName = field.type.canonicalText
        return "${field.simpleName} = ($typeName) $parcel.readValue($typeName.class.getClassLoader());"
    }
}


