package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiType
import org.hz.cauchy.plugin.typeserializers.serializers.BundleSerializer

/**
 * Bundle序列化器工厂
 * 处理Bundle类型
 */
class BundleSerializerFactory : TypeSerializerFactory {

    override fun getSerializer(psiType: PsiType): TypeSerializer? {
        return if (psiType.canonicalText == "android.os.Bundle") {
            BundleSerializer()
        } else {
            null
        }
    }
}


