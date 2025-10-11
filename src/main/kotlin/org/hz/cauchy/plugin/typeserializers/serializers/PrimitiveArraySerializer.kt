package org.hz.cauchy.plugin.typeserializers.serializers

import org.hz.cauchy.plugin.typeserializers.SerializableValue
import org.hz.cauchy.plugin.typeserializers.TypeSerializer

/**
 * 基本类型数组序列化器
 * 处理int[], long[], float[], double[], boolean[], byte[], char[]等基本类型数组
 */
class PrimitiveArraySerializer(private val typeName: String) : TypeSerializer {

    override fun writeValue(field: SerializableValue, parcel: String, flags: String): String {
        return "$parcel.write${typeName}Array(${field.name});"
    }

    override fun readValue(field: SerializableValue, parcel: String): String {
        return "${field.simpleName} = $parcel.create${typeName}Array();"
    }
}
