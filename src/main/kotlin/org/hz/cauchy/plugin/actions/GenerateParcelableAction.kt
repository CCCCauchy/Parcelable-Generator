package org.hz.cauchy.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.hz.cauchy.plugin.generator.CodeGenerator


/**
 * 为Java Bean类生成Parcelable接口实现的Action
 */
class GenerateParcelableAction : AnAction() {

    // 指定Action更新线程为后台线程
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val psiClass = getPsiClassFromContext(e) ?: return

        // 直接获取所有非静态非瞬态字段，不显示弹窗
        val fields = getAllSerializableFields(psiClass)
        generateParcelable(psiClass, fields)
    }

    private fun generateParcelable(psiClass: PsiClass, fields: List<PsiField>) {
        WriteCommandAction.runWriteCommandAction(psiClass.project) {
            CodeGenerator(psiClass, fields).generate()
        }
    }

    /**
     * 获取所有可序列化的字段（包括父类字段）
     * 排除静态字段和瞬态字段
     */
    private fun getAllSerializableFields(psiClass: PsiClass): List<PsiField> {
        return psiClass.allFields.filter { field ->
            !field.hasModifierProperty(PsiModifier.STATIC) && 
            !field.hasModifierProperty(PsiModifier.TRANSIENT)
        }
    }

    private fun getPsiClassFromContext(e: AnActionEvent): PsiClass? {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (psiFile == null || editor == null) {
            return null
        }

        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        
        return PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
    }

    override fun update(e: AnActionEvent) {
        val psiClass = getPsiClassFromContext(e)
        e.presentation.isEnabledAndVisible = psiClass != null && !psiClass.isEnum && !psiClass.isInterface
    }
}