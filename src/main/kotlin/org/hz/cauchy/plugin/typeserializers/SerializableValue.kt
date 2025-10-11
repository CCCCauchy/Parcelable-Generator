package org.hz.cauchy.plugin.typeserializers

import com.intellij.psi.PsiField
import com.intellij.psi.PsiType

/**
 * 可序列化值的抽象类
 * 用于封装字段信息，支持不同类型的序列化值
 */
abstract class SerializableValue {
    abstract val type: PsiType
    abstract val name: String
    abstract val simpleName: String

    companion object {
        /**
         * 从PsiField创建SerializableValue
         */
        fun member(field: PsiField): SerializableValue = MemberSerializableValue(field)

        /**
         * 从语句创建SerializableValue
         */
        fun statement(statement: String, type: PsiType): SerializableValue = StatementSerializableValue(statement, type)

        /**
         * 从变量名创建SerializableValue
         */
        fun variable(name: String, type: PsiType): SerializableValue = VariableSerializableValue(name, type)
    }

    /**
     * 成员字段的SerializableValue实现
     */
    private class MemberSerializableValue(private val field: PsiField) : SerializableValue() {
        override val type: PsiType = field.type
        override val name: String = "this.${field.name}"
        override val simpleName: String = field.name
    }

    /**
     * 语句的SerializableValue实现
     */
    private class StatementSerializableValue(
        private val statement: String,
        private val statementType: PsiType
    ) : SerializableValue() {
        override val type: PsiType = statementType
        override val name: String = statement
        override val simpleName: String = statement
    }

    /**
     * 变量的SerializableValue实现
     */
    private class VariableSerializableValue(
        private val variableName: String,
        private val variableType: PsiType
    ) : SerializableValue() {
        override val type: PsiType = variableType
        override val name: String = "${variableType.canonicalText} $variableName"
        override val simpleName: String = variableName
    }
}
