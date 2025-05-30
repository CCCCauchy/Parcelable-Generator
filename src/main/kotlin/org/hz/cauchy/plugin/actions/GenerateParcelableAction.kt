package org.hz.cauchy.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil


/**
 * 为Java Bean类生成Parcelable接口实现的Action
 */
class GenerateParcelableAction : AnAction() {


    // 指定Action更新线程为后台线程
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        if (psiFile !is PsiJavaFile) {
            return
        }
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: return
        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        generateParcelableImplementation(project, psiClass)
    }

    private fun generateParcelableImplementation(project: Project, psiClass: PsiClass) {
        WriteCommandAction.runWriteCommandAction(project) {
            val factory = PsiElementFactory.getInstance(project)

            // 获取所有非静态、非瞬态字段
            val fields = psiClass.fields.filter { field ->
                !field.hasModifierProperty(PsiModifier.STATIC) && !field.hasModifierProperty(
                    PsiModifier.TRANSIENT
                )
            }

            // 删除已存在的序列化方法和字段
            try {
                // 查找所有可能的Parcel构造函数并删除
                val parcelConstructors = psiClass.constructors.filter { constructor ->
                    constructor.parameterList.parametersCount == 0 ||
                            (constructor.parameterList.parametersCount == 1 &&
                                    constructor.parameterList.parameters[0].type.presentableText.contains(
                                        "Parcel"
                                    ))
                }

                parcelConstructors.forEach {
                    it.delete()
                }

                // 查找并删除describeContents方法
                val describeContentsMethods = psiClass.methods.filter {
                    it.name == "describeContents"
                }

                describeContentsMethods.forEach {
                    it.delete()
                }

                // 查找并删除writeToParcel方法
                val writeToParcelMethods = psiClass.methods.filter {
                    it.name == "writeToParcel"
                }

                writeToParcelMethods.forEach {
                    it.delete()
                }

                // 查找并删除CREATOR字段
                val creatorFields = psiClass.fields.filter {
                    it.name == "CREATOR"
                }

                creatorFields.forEach {
                    it.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 准备所有需要添加的元素
            val elementsToAdd = mutableListOf<Pair<String, (String) -> PsiElement>>()

            // 准备默认无参构造方法
            val defaultConstructorText = "public ${psiClass.name}() {\n}"
            elementsToAdd.add(Pair(defaultConstructorText) { text ->
                factory.createMethodFromText(
                    text,
                    psiClass
                )
            })

            // 准备describeContents方法
            val describeContentsText = """
                @Override
                public int describeContents() {
                    return 0;
                }
            """.trimIndent()
            elementsToAdd.add(Pair(describeContentsText) { text ->
                factory.createMethodFromText(
                    text,
                    psiClass
                )
            })

            // 准备writeToParcel方法
            val writeToParcelText = buildString {
                append("@Override\n")
                append("public void writeToParcel(android.os.Parcel dest, int flags) {\n")
                // 检查是否是第一个字段，则添加super调用
                if (psiClass.superClass?.methods?.any { it.name == "writeToParcel" } == true) {
                    append("super.writeToParcel(dest, flags);\n")
                }

                for (field in fields) {
                    append(getWriteParcelStatement(field))
                }
                append("}")
            }
            elementsToAdd.add(Pair(writeToParcelText) { text ->
                factory.createMethodFromText(
                    text,
                    psiClass
                )
            })

            // 准备Parcel构造函数
            val constructorText = buildString {
                append("protected ${psiClass.name}(android.os.Parcel in) {\n")

                // 检查是否是第一个字段，如果是且父类实现了Parcelable，则添加super调用
                if (psiClass.superClass?.constructors?.any { constructor ->
                        constructor.parameterList.parametersCount == 1 &&
                                constructor.parameterList.parameters[0].type.presentableText.contains(
                                    "Parcel"
                                )
                    } == true) {
                    append("super(in);\n")
                }

                for (field in fields) {
                    append(getReadParcelStatement(field))
                }
                append("}")
            }
            elementsToAdd.add(Pair(constructorText) { text ->
                factory.createMethodFromText(
                    text,
                    psiClass
                )
            })

            // 准备CREATOR字段 - 添加到最后
            val creatorText = """
                public static final android.os.Parcelable.Creator<${psiClass.name}> CREATOR = new android.os.Parcelable.Creator<${psiClass.name}>() {
                    @Override
                    public ${psiClass.name} createFromParcel(android.os.Parcel source) {
                        return new ${psiClass.name}(source);
                    }

                    @Override
                    public ${psiClass.name}[] newArray(int size) {
                        return new ${psiClass.name}[size];
                    }
                };
            """.trimIndent()
            elementsToAdd.add(Pair(creatorText) { text ->
                factory.createFieldFromText(
                    text,
                    psiClass
                )
            })

            // 找到合适的锚点元素（确保锚点在类内部）
            // 添加所有元素
            elementsToAdd.forEach { (text, creator) ->
                val newElement = creator(text)

                psiClass.addBefore(newElement, psiClass.rBrace)
            }

        }
    }

    private fun getReadParcelStatement(field: PsiField): String {
        val name = field.name
        val type = field.type
        val typeName = field.type.canonicalText

        val elementType = PsiUtil.extractIterableTypeParameter(field.type, false)
        if (elementType != null) {
            val elementTypeName = elementType.presentableText
            return when {
                elementTypeName == "String" -> "this.$name = in.createStringArrayList();\n"
                isImplementParcelable(elementType) -> "this.$name = in.createTypedArrayList(${elementTypeName}.CREATOR);\n"
                else -> "this.$name = new ArrayList<>();\n" +
                        "in.readList(this.$name, $elementTypeName.class.getClassLoader());\n"
            }
        }

        return when {
            typeName == "int" -> "this.$name = in.readInt();\n"
            typeName == "boolean" -> "this.$name = in.readByte() != 0;\n"
            typeName == "byte" -> "this.$name = in.readByte();\n"
            typeName == "char" -> "this.$name = (char) in.readInt();\n"
            typeName == "long" -> "this.$name = in.readLong();\n"
            typeName == "float" -> "this.$name = in.readFloat();\n"
            typeName == "double" -> "this.$name = in.readDouble();\n"
            typeName == "short" -> "this.$name = (short) in.readInt();\n"
            typeName == "java.lang.String" -> "this.$name = in.readString();\n"
            isImplementCharSequence(field.type) -> "this.$name = ($typeName) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);\n"

            // 添加Map支持
            typeName.startsWith("java.util.Map") || typeName.startsWith("java.util.HashMap") -> {
                "this.$name = new HashMap<>();\n" +
                        "in.readMap(this.$name, ${(type as? PsiClassType)?.parameters?.get(1)?.presentableText ?: "Object"}.class.getClassLoader());\n"
            }

            typeName.endsWith("[]") -> {
                "in.readTypedArray(this.$name, ${getArrayCreatorExpression(typeName)});\n"
            }

            isImplementParcelable(field.type) -> {
                "this.$name = in.readParcelable($typeName.class.getClassLoader());\n"
            }

            else -> {
                "/*** TODO Could not determine how to read {@link $name} of type [$typeName] */\n"
            }
        }
    }

    private fun getWriteParcelStatement(field: PsiField): String {
        val name = field.name
        val typeName = field.type.canonicalText

        val elementType = PsiUtil.extractIterableTypeParameter(field.type, false)
        if (elementType != null) {
            return if (elementType.canonicalText == "java.lang.String")
                "dest.writeStringList(this.$name);\n"
            else if (isImplementParcelable(elementType)) "dest.writeTypedList(this.$name);\n"
            else "dest.writeList(this.$name);\n"
        }

        return when {
            typeName == "int" -> "dest.writeInt(this.$name);\n"
            typeName == "boolean" -> "dest.writeByte(this.$name ? (byte) 1 : (byte) 0);\n"
            typeName == "byte" -> "dest.writeByte(this.$name);\n"
            typeName == "char" -> "dest.writeInt((int) this.$name);\n"
            typeName == "long" -> "dest.writeLong(this.$name);\n"
            typeName == "float" -> "dest.writeFloat(this.$name);\n"
            typeName == "double" -> "dest.writeDouble(this.$name);\n"
            typeName == "short" -> "dest.writeInt((int) this.$name);\n"
            typeName == "java.lang.String" -> "dest.writeString(this.$name);\n"
            isImplementCharSequence(field.type) -> "TextUtils.writeToParcel(this.$name, dest, 0);\n"


            // 添加Map支持
            typeName.startsWith("java.util.Map") || typeName.startsWith("java.util.HashMap") -> {
                "dest.writeMap(this.$name);\n"
            }

            typeName.endsWith("[]") -> {
                "dest.writeTypedArray(this.$name, flags);\n"
            }

            isImplementParcelable(field.type) -> {
                "dest.writeParcelable(this.$name, flags);\n"
            }

            else -> {
                "/*** TODO Could not determine how to write {@link $name} of type [$typeName] */\n"
            }
        }
    }

    private fun getArrayCreatorExpression(type: String): String {
        val baseType = type.substring(0, type.length - 2)
        return "$baseType.CREATOR"
    }

    /**
     * 检查类是否实现了Parcelable接口
     */
    private fun isImplementParcelable(elementType: PsiType): Boolean {
        if (elementType is PsiClassType) {
            val psiClass = elementType.resolve() ?: return false
            return isImplementParcelable(psiClass)
        }

        return false
    }

    /**
     * 检查类是否实现了Parcelable接口
     */
    private fun isImplementParcelable(psiClass: PsiClass): Boolean {
        val parcelableClass: PsiClass = JavaPsiFacade.getInstance(psiClass.project)
            .findClass("android.os.Parcelable", GlobalSearchScope.allScope(psiClass.project))
            ?: return false

        return psiClass.isInheritor(parcelableClass, true)
    }

    /**
     * 检查类是否实现了CharSequence接口
     */
    private fun isImplementCharSequence(elementType: PsiType): Boolean {
        if (elementType is PsiClassType) {
            val psiClass = elementType.resolve() ?: return false
            val charSequenceClass: PsiClass = JavaPsiFacade.getInstance(psiClass.project)
                .findClass("java.lang.CharSequence", GlobalSearchScope.allScope(psiClass.project))
                ?: return false

            return psiClass.isInheritor(charSequenceClass, true)
        }

        return false
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        var available = false

        if (project != null && editor != null && psiFile is PsiJavaFile) {
            val element = psiFile.findElementAt(editor.caretModel.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
            available = psiClass != null
        }

        e.presentation.isEnabledAndVisible = available
    }
}