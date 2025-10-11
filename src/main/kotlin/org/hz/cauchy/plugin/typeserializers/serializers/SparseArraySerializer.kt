package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * SparseArray序列化器
 * 处理SparseArray类型
 */
class SparseArraySerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeSparseArray(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = $parcel.readSparseArray(java.lang.Object.class.getClassLoader());"
    }
}


