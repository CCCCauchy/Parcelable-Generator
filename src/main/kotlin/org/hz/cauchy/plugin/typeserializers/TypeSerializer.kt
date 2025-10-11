package org.hz.cauchy.plugin.typeserializers

/**
 * 类型序列化器接口
 * 用于生成Parcelable的序列化和反序列化代码
 */
interface TypeSerializer {
    /**
     * 生成写入Parcel的代码
     * @param field 字段信息
     * @param parcel Parcel变量名
     * @param flags flags参数名
     * @return 生成的代码字符串
     */
    fun writeValue(field: SerializableValue, parcel: String, flags: String): String

    /**
     * 生成从Parcel读取的代码
     * @param field 字段信息
     * @param parcel Parcel变量名
     * @return 生成的代码字符串
     */
    fun readValue(field: SerializableValue, parcel: String): String
}

