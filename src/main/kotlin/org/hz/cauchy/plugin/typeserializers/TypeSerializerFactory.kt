package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType

/**
 * 类型序列化器工厂接口
 * 用于根据PsiType获取对应的序列化器
 */
interface TypeSerializerFactory {
    /**
     * 根据PsiType获取对应的序列化器
     * @param psiType 要序列化的类型
     * @return 对应的序列化器，如果没有找到则返回null
     */
    fun getSerializer(psiType: PsiType): TypeSerializer?
}

