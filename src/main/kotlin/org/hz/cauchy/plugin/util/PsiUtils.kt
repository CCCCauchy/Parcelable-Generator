package org.hz.cauchy.plugin.util

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType

/**
 * Psi工具类
 * 提供PSI相关的工具方法
 */
object PsiUtils {

    /**
     * 检查类型是否是指定的类型
     */
    fun isOfType(psiType: PsiType, qualifiedName: String): Boolean {
        if (psiType is PsiClassType) {
            val resolved = psiType.resolve()
            return resolved?.qualifiedName == qualifiedName
        }
        return false
    }
}


