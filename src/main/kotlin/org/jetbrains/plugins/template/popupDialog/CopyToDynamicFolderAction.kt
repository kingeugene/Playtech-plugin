package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.template.branding.brandService

class CopyToDynamicFolderAction(folderLabel: String, private val folderName: String) : AnAction("Copy to $folderLabel") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val singleFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null) {
            presentation.isEnabledAndVisible = false
            return
        }

        // Handle multi-selection or single selection
        val selectedFiles = files?.toList() ?: (singleFile?.let { listOf(it) } ?: emptyList())
        
        if (selectedFiles.isEmpty()) {
            presentation.isEnabledAndVisible = false
            return
        }

        // Check if selection contains any directories
        val hasDirectories = selectedFiles.any { it.isDirectory }
        
        // If selection contains directories, always enable (don't check target existence)
        if (hasDirectories) {
            val targetRoot = project.brandService.findRootByName(folderName)
            if (targetRoot == null) {
                presentation.isEnabledAndVisible = false
                return
            }
            presentation.isVisible = true
            presentation.isEnabled = true
            presentation.description = "Copy to ${targetRoot.displayName}"
            return
        }

        // For files only: check target existence and disable if exists
        val file = selectedFiles.firstOrNull()
        if (file == null || file.isDirectory) {
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
