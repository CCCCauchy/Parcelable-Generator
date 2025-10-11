package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * Bundle序列化器
 * 特殊处理Bundle类型
 */
class BundleSerializer : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.writeBundle(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = $parcel.readBundle();"
    }
}


