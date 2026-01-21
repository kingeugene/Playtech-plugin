package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.template.branding.brandService

class CopyToDynamicFolderAction(folderLabel: String, private val folderName: String) : AnAction("Copy to $folderLabel") {

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project: Project? = e.project
        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || file == null || file.isDirectory) {
            presentation.isEnabledAndVisible = false
            return
        }

        val brandService = project.brandService
        val context = brandService.getBrandContext(file)
        val targetRoot = brandService.findRootByName(folderName)

        if (context == null || targetRoot == null) {
            presentation.isEnabledAndVisible = false
            return
        }

        val alreadyHere = context.currentRoot.name == targetRoot.name
        val targetExists = targetRoot.root.findFileByRelativePath(context.relativePath) != null

        presentation.isVisible = true
        presentation.isEnabled = !alreadyHere && !targetExists
        presentation.description = when {
            alreadyHere -> "File is already in ${targetRoot.displayName}"
            targetExists -> "File already exists in ${targetRoot.displayName}"
            else -> "Copy to ${targetRoot.displayName}"
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        copyFileToFolder(event, folderName)
    }
}
