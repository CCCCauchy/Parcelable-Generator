package org.hz.cauchy.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil

/**
 * 为Java Bean类生成Parcelable接口实现的Action
 */
class GenerateParcelableAction : AnAction() {

    private val LOG = Logger.getInstance(GenerateParcelableAction::class.java)

    // 指定Action更新线程为后台线程
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.info("GenerateParcelableAction被触发")
        val project = e.project
        if (project == null) {
            LOG.warn("未找到项目，操作取消")
            return
        }

        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            LOG.warn("未找到编辑器，操作取消")
            return
        }

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile == null) {
            LOG.warn("未找到PSI文件，操作取消")
            return
        }

        if (psiFile !is PsiJavaFile) {
            LOG.warn("文件不是Java文件，操作取消")
            return
        }

        LOG.info("找到Java文件: ${psiFile.name}")
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        if (element == null) {
            LOG.warn("光标位置没有找到元素，操作取消")
            return
        }

        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        if (psiClass == null) {
            LOG.warn("未找到Java类，操作取消")
            return
        }

        LOG.info("找到Java类: ${psiClass.name}")

        generateParcelableImplementation(project, psiClass, element)
        LOG.info("成功为类 ${psiClass.name} 生成Parcelable实现")
    }

    private fun findParcelableInterface(project: Project): PsiClass? {
        LOG.info("查找Parcelable接口")
        return PsiTypesUtil.getPsiClass(
            PsiElementFactory.getInstance(project).createTypeByFQClassName(
                "android.os.Parcelable",
                GlobalSearchScope.allScope(project)
            )
        )
    }

    private fun generateParcelableImplementation(project: Project, psiClass: PsiClass, anchorElement: PsiElement) {
        LOG.info("开始生成Parcelable实现")
        WriteCommandAction.runWriteCommandAction(project) {
            val factory = PsiElementFactory.getInstance(project)

            // 获取所有非静态、非瞬态字段
            val fields = psiClass.fields.filter { field ->
                !field.hasModifierProperty(PsiModifier.STATIC) && !field.hasModifierProperty(PsiModifier.TRANSIENT)
            }
            LOG.info("找到 ${fields.size} 个需要处理的字段")

            // 删除已存在的序列化方法和字段
            LOG.info("检查并删除已存在的序列化方法和字段")

            try {
                // 查找所有可能的Parcel构造函数并删除
                LOG.info("查找并删除现有的Parcel构造函数")
                val parcelConstructors = psiClass.constructors.filter { constructor ->
                    constructor.parameterList.parametersCount == 0 ||
                            (constructor.parameterList.parametersCount == 1 &&
                                    constructor.parameterList.parameters[0].type.presentableText.contains("Parcel"))
                }

                parcelConstructors.forEach {
                    LOG.info("删除Parcel构造函数: ${it.text}")
                    it.delete()
                }

                // 查找并删除describeContents方法
                LOG.info("查找并删除现有的describeContents方法")
                val describeContentsMethods = psiClass.methods.filter {
                    it.name == "describeContents"
                }

                describeContentsMethods.forEach {
                    LOG.info("删除describeContents方法: ${it.text}")
                    it.delete()
                }

                // 查找并删除writeToParcel方法
                LOG.info("查找并删除现有的writeToParcel方法")
                val writeToParcelMethods = psiClass.methods.filter {
                    it.name == "writeToParcel"
                }

                writeToParcelMethods.forEach {
                    LOG.info("删除writeToParcel方法: ${it.text}")
                    it.delete()
                }

                // 查找并删除CREATOR字段
                LOG.info("查找并删除现有的CREATOR字段")
                val creatorFields = psiClass.fields.filter {
                    it.name == "CREATOR"
                }

                creatorFields.forEach {
                    LOG.info("删除CREATOR字段: ${it.text}")
                    it.delete()
                }
            } catch (e: Exception) {
                LOG.error("删除现有序列化方法时出错", e)
            }

            // 准备所有需要添加的元素
            val elementsToAdd = mutableListOf<Pair<String, (String) -> PsiElement>>()

            // 准备默认无参构造方法
            LOG.info("准备默认无参构造方法")
            val defaultConstructorText = "public ${psiClass.name}() {\n}"
            elementsToAdd.add(Pair(defaultConstructorText) { text -> factory.createMethodFromText(text, psiClass) })

            // 准备describeContents方法
            LOG.info("准备describeContents方法")
            val describeContentsText = """
                @Override
                public int describeContents() {
                    return 0;
                }
            """.trimIndent()
            elementsToAdd.add(Pair(describeContentsText) { text -> factory.createMethodFromText(text, psiClass) })

            // 准备writeToParcel方法
            LOG.info("准备writeToParcel方法")
            val writeToParcelText = buildString {
                append("@Override\n")
                append("public void writeToParcel(android.os.Parcel dest, int flags) {\n")
                for (field in fields) {
                    LOG.info("为字段 ${field.name} (${field.type.canonicalText}) 生成写入代码")
                    append(getWriteParcelStatement(field))
                }
                append("}")
            }
            elementsToAdd.add(Pair(writeToParcelText) { text -> factory.createMethodFromText(text, psiClass) })

            // 准备Parcel构造函数
            LOG.info("准备Parcel构造函数")
            val constructorText = buildString {
                append("protected ${psiClass.name}(android.os.Parcel in) {\n")
                for (field in fields) {
                    LOG.info("为字段 ${field.name} (${field.type.canonicalText}) 生成读取代码")
                    append(getReadParcelStatement(field))
                }
                append("}")
            }
            elementsToAdd.add(Pair(constructorText) { text -> factory.createMethodFromText(text, psiClass) })

            // 准备CREATOR字段 - 添加到最后
            LOG.info("准备CREATOR字段")
            val creatorText = """
                public static final android.os.Parcelable.Creator<${psiClass.name}> CREATOR = new android.os.Parcelable.Creator<${psiClass.name}>() {
                    @Override
                    public ${psiClass.name} createFromParcel(android.os.Parcel in) {
                        return new ${psiClass.name}(in);
                    }

                    @Override
                    public ${psiClass.name}[] newArray(int size) {
                        return new ${psiClass.name}[size];
                    }
                };
            """.trimIndent()
            elementsToAdd.add(Pair(creatorText) { text -> factory.createFieldFromText(text, psiClass) })

            // 找到合适的锚点元素（确保锚点在类内部）
            LOG.info("确定插入位置")


            // 添加所有元素
            elementsToAdd.forEach { (text, creator) ->
                val newElement = creator(text)

                psiClass.addBefore(newElement, psiClass.rBrace)
            }

            LOG.info("Parcelable实现生成完成")
        }
    }

    private fun getReadParcelStatement(field: PsiField): String {
        val name = field.name
        val type = field.type.canonicalText
        LOG.info("为字段 $name 处理Parcel读取, 类型: $type")

        return when {
            type == "int" -> "this.$name = in.readInt();\n"
            type == "boolean" -> "this.$name = in.readByte() != 0;\n"
            type == "byte" -> "this.$name = in.readByte();\n"
            type == "char" -> "this.$name = (char) in.readInt();\n"
            type == "long" -> "this.$name = in.readLong();\n"
            type == "float" -> "this.$name = in.readFloat();\n"
            type == "double" -> "this.$name = in.readDouble();\n"
            type == "short" -> "this.$name = (short) in.readInt();\n"
            type == "java.lang.String" -> "this.$name = in.readString();\n"
            type.startsWith("java.util.List") || type.startsWith("java.util.ArrayList") -> {
                LOG.info("处理集合类型: $type")
                "this.$name = in.readArrayList(getClass().getClassLoader());\n"
            }

            type.endsWith("[]") -> {
                LOG.info("处理数组类型: $type")
                "in.readTypedArray(this.$name, ${getArrayCreatorExpression(type)});\n"
            }

            isParcelable(type) -> {
                LOG.info("处理Parcelable类型: $type")
                "this.$name = in.readParcelable(getClass().getClassLoader());\n"
            }

            else -> {
                LOG.warn("无法确定如何读取类型为 $type 的字段 $name")
                "// Could not determine how to read $name of type $type\n"
            }
        }
    }

    private fun getWriteParcelStatement(field: PsiField): String {
        val name = field.name
        val type = field.type.canonicalText
        LOG.info("为字段 $name 处理Parcel写入, 类型: $type")

        return when {
            type == "int" -> "dest.writeInt(this.$name);\n"
            type == "boolean" -> "dest.writeByte(this.$name ? (byte) 1 : (byte) 0);\n"
            type == "byte" -> "dest.writeByte(this.$name);\n"
            type == "char" -> "dest.writeInt((int) this.$name);\n"
            type == "long" -> "dest.writeLong(this.$name);\n"
            type == "float" -> "dest.writeFloat(this.$name);\n"
            type == "double" -> "dest.writeDouble(this.$name);\n"
            type == "short" -> "dest.writeInt((int) this.$name);\n"
            type == "java.lang.String" -> "dest.writeString(this.$name);\n"
            type.startsWith("java.util.List") || type.startsWith("java.util.ArrayList") -> {
                LOG.info("处理集合类型: $type")
                "dest.writeList(this.$name);\n"
            }

            type.endsWith("[]") -> {
                LOG.info("处理数组类型: $type")
                "dest.writeTypedArray(this.$name, flags);\n"
            }

            isParcelable(type) -> {
                LOG.info("处理Parcelable类型: $type")
                "dest.writeParcelable(this.$name, flags);\n"
            }

            else -> {
                LOG.warn("无法确定如何写入类型为 $type 的字段 $name")
                "// Could not determine how to write $name of type $type\n"
            }
        }
    }

    private fun getArrayCreatorExpression(type: String): String {
        val baseType = type.substring(0, type.length - 2)
        LOG.info("生成数组CREATOR表达式: $baseType.CREATOR")
        return "$baseType.CREATOR"
    }

    private fun isParcelable(type: String): Boolean {
        // 简单判断是否实现了Parcelable接口
        // 在实际应用中，应该检查类是否真正实现了Parcelable接口
        val result = !type.startsWith("java.") && !type.startsWith("javax.") &&
                !type.equals("int") && !type.equals("boolean") &&
                !type.equals("byte") && !type.equals("char") &&
                !type.equals("long") && !type.equals("float") &&
                !type.equals("double") && !type.equals("short")
        LOG.info("检查类型 $type 是否为Parcelable: $result")
        return result
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
        LOG.info("操作可用性更新: $available")
    }
} 