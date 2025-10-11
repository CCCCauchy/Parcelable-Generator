package org.hz.cauchy.plugin.generator

import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.hz.cauchy.plugin.typeserializers.*
import org.hz.cauchy.plugin.typeserializers.serializers.ParcelableObjectSerializer
import org.hz.cauchy.plugin.util.PsiUtils

/**
 * 代码生成器
 * 使用新的序列化器架构生成Parcelable代码
 */
class CodeGenerator(
    private val psiClass: PsiClass,
    private val fields: List<PsiField>
) {
    companion object {
        const val CREATOR_NAME = "CREATOR"
        const val TYPE_PARCEL = "android.os.Parcel"
    }

    private val typeSerializerFactory: TypeSerializerFactory

    init {
        val baseChain = ChainSerializerFactory(
            BundleSerializerFactory(),
            DateSerializerFactory(),
            EnumerationSerializerFactory(),
            PrimitiveTypeSerializerFactory(),
            PrimitiveArraySerializerFactory(),
            ParcelableSerializerFactory(),
            ListSerializerFactory(),
            SerializableSerializerFactory(),
            SparseArraySerializerFactory()
        )
        this.typeSerializerFactory = baseChain.extend(MapSerializerFactory())
    }

    /**
     * 生成Parcelable实现
     */
    fun generate() {
        val elementFactory = JavaPsiFacade.getElementFactory(psiClass.project)

        removeExistingParcelableImplementation(psiClass)

        // 生成describeContents方法
        val describeContentsMethod = elementFactory.createMethodFromText(generateDescribeContents(), psiClass)
        
        // 生成writeToParcel方法
        val writeToParcelMethod = elementFactory.createMethodFromText(generateWriteToParcel(fields), psiClass)

        // 生成默认构造函数（如果需要）
        val defaultConstructorString = generateDefaultConstructor(psiClass)
        val defaultConstructor = if (defaultConstructorString != null) {
            elementFactory.createMethodFromText(defaultConstructorString, psiClass)
        } else null

        // 生成Parcel构造函数
        val constructor = elementFactory.createMethodFromText(generateConstructor(fields, psiClass), psiClass)
        
        // 生成CREATOR字段
        val creatorField = elementFactory.createFieldFromText(generateStaticCreator(psiClass), psiClass)

        val styleManager = JavaCodeStyleManager.getInstance(psiClass.project)

        // 添加所有元素并优化引用
        styleManager.shortenClassReferences(psiClass.addBefore(describeContentsMethod, psiClass.lastChild))
        styleManager.shortenClassReferences(psiClass.addBefore(writeToParcelMethod, psiClass.lastChild))

        if (defaultConstructor != null) {
            styleManager.shortenClassReferences(psiClass.addBefore(defaultConstructor, psiClass.lastChild))
        }

        styleManager.shortenClassReferences(psiClass.addBefore(constructor, psiClass.lastChild))
        styleManager.shortenClassReferences(psiClass.addBefore(creatorField, psiClass.lastChild))

        makeClassImplementParcelable(elementFactory)
    }

    /**
     * 生成describeContents方法
     */
    private fun generateDescribeContents(): String {
        return "@Override public int describeContents() { return 0; }"
    }

    /**
     * 生成writeToParcel方法
     */
    private fun generateWriteToParcel(fields: List<PsiField>): String {
        val sb = StringBuilder()
        sb.append("@Override public void writeToParcel(android.os.Parcel dest, int flags) {")
        
        // 如果存在父类且父类实现了Parcelable，则调用父类的writeToParcel方法
        if (hasParcelableSuperclass()) {
            sb.append("super.writeToParcel(dest, flags);")
        }
        
        // 只处理当前类声明的字段，不包括父类字段
        val currentClassFields = fields.filter { field ->
            field.containingClass == psiClass
        }
        
        for (field in currentClassFields) {
            val serializer = getSerializerForType(field)
            sb.append(serializer.writeValue(SerializableValue.member(field), "dest", "flags"))
        }
        
        sb.append("}")
        return sb.toString()
    }

    /**
     * 生成Parcel构造函数
     */
    private fun generateConstructor(fields: List<PsiField>, psiClass: PsiClass): String {
        val className = psiClass.name
        val sb = StringBuilder()
        
        sb.append("protected $className(android.os.Parcel in) {")
        
        // 如果存在父类且父类有Parcel构造函数，则调用父类构造函数
        if (hasParcelableSuperclass() && hasParcelableSuperConstructor()) {
            sb.append("super(in);")
        }
        
        // 只处理当前类声明的字段，不包括父类字段
        val currentClassFields = fields.filter { field ->
            field.containingClass == psiClass
        }
        
        for (field in currentClassFields) {
            val serializer = getSerializerForType(field)
            sb.append(serializer.readValue(SerializableValue.member(field), "in"))
        }
        
        sb.append("}")
        return sb.toString()
    }

    /**
     * 生成静态CREATOR字段
     */
    private fun generateStaticCreator(psiClass: PsiClass): String {
        val className = psiClass.name
        return "public static final android.os.Parcelable.Creator<$className> CREATOR = new android.os.Parcelable.Creator<$className>(){@Override public $className createFromParcel(android.os.Parcel source) {return new $className(source);}@Override public $className[] newArray(int size) {return new $className[size];}};"
    }

    /**
     * 生成默认构造函数
     */
    private fun generateDefaultConstructor(clazz: PsiClass): String? {
        return if (clazz.constructors.isEmpty()) {
            "public ${clazz.name}(){}"
        } else {
            null
        }
    }

    /**
     * 获取字段对应的序列化器
     */
    private fun getSerializerForType(field: PsiField): TypeSerializer {
        return typeSerializerFactory.getSerializer(field.type) ?: ParcelableObjectSerializer()
    }

    /**
     * 检查是否有Parcelable父类
     */
    private fun hasParcelableSuperclass(): Boolean {
        val superClass = psiClass.superClass ?: return false
        val superTypes = superClass.superTypes
        for (superType in superTypes) {
            if (PsiUtils.isOfType(superType, "android.os.Parcelable")) {
                return true
            }
        }
        return false
    }

    /**
     * 检查是否有Parcel构造函数
     */
    private fun hasParcelableSuperConstructor(): Boolean {
        val superClass = psiClass.superClass ?: return false
        val constructors = superClass.constructors
        for (constructor in constructors) {
            val parameterList = constructor.parameterList
            if (parameterList.parametersCount == 1 &&
                parameterList.parameters[0].type.canonicalText == TYPE_PARCEL) {
                return true
            }
        }
        return false
    }

    /**
     * 检查是否有指定的父类方法
     */
    private fun hasSuperMethod(methodName: String): Boolean {
        if (methodName.isEmpty()) return false

        val superClass = psiClass.superClass ?: return false
        val superclassMethods = superClass.allMethods
        for (superclassMethod in superclassMethods) {
            if (superclassMethod.body == null) continue

            val name = superclassMethod.name
            if (name == methodName) {
                return true
            }
        }
        return false
    }

    /**
     * 移除现有的Parcelable实现
     */
    private fun removeExistingParcelableImplementation(psiClass: PsiClass) {
        val allFields = psiClass.allFields

        // 查找并删除现有的CREATOR
        for (field in allFields) {
            if (field.name == CREATOR_NAME) {
                field.delete()
            }
        }

        findAndRemoveMethod(psiClass, psiClass.name ?: "", TYPE_PARCEL)
        findAndRemoveMethod(psiClass, "describeContents")
        findAndRemoveMethod(psiClass, "writeToParcel", TYPE_PARCEL, "int")
    }

    /**
     * 让类实现Parcelable接口
     */
    private fun makeClassImplementParcelable(elementFactory: PsiElementFactory) {
        if (hasParcelableSuperclass()) return

        val implementsListTypes = psiClass.implementsListTypes
        val implementsType = "android.os.Parcelable"

        for (implementsListType in implementsListTypes) {
            val resolved = implementsListType.resolve()
            if (resolved != null && implementsType == resolved.qualifiedName) {
                return
            }
        }

        val implementsReference = elementFactory.createReferenceFromText(implementsType, psiClass)
        val implementsList = psiClass.implementsList

        if (implementsList != null) {
            implementsList.add(implementsReference)
        }
    }

    /**
     * 查找并删除指定方法
     */
    private fun findAndRemoveMethod(clazz: PsiClass, methodName: String, vararg arguments: String) {
        val methods = clazz.findMethodsByName(methodName, false)

        for (method in methods) {
            val parameterList = method.parameterList

            if (parameterList.parametersCount == arguments.size) {
                var shouldDelete = true

                val parameters = parameterList.parameters

                for (i in arguments.indices) {
                    if (parameters[i].type.canonicalText != arguments[i]) {
                        shouldDelete = false
                    }
                }

                if (shouldDelete) {
                    method.delete()
                }
            }
        }
    }
}
