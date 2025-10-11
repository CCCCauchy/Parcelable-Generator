package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Map序列化器
 * 处理Map类型的序列化
 */
class MapSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeMap(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = new java.util.HashMap<>();\n" +
               "$parcel.readMap(${field.simpleName}, java.lang.Object.class.getClassLoader());"
    }
}


