package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.template.branding.BrandRoot
import org.jetbrains.plugins.template.branding.brandService

fun copyFileToFolder(event: AnActionEvent, targetFolderName: String) {
    val project: Project = event.project ?: return showErrorLater("Project not found")

    val selectedFile: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        ?: return showErrorLater("No file selected")

    if (selectedFile.isDirectory) {
        return showErrorLater("Copy from directory is not supported. Please select a file.")
    }

    val brandService = project.brandService
    val context = brandService.getBrandContext(selectedFile)
        ?: return showErrorLater("Selected file is not inside a recognized theme root")

    val targetRoot: BrandRoot = brandService.findRootByName(targetFolderName)
        ?: return showErrorLater("Target folder '$targetFolderName' not found in project root")

    if (targetRoot.name == context.currentRoot.name) {
        return showErrorLater("File is already in the target brand")
    }

    val existing = targetRoot.root.findFileByRelativePath(context.relativePath)
    if (existing != null) {
        return showErrorLater("File already exists in '${targetRoot.displayName}'")
    }

    brandService.copyToBrand(context, targetRoot) { created ->
        if (created == null) {
            Messages.showErrorDialog(project, "Failed to copy file to ${targetRoot.displayName}", "Error")
        } else {
            Messages.showInfoMessage(project, "Copied to ${targetRoot.displayName}", "Success")
        }
    }
}

private fun showErrorLater(message: String) {
    ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(message, "Error")
    }
}
