package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.plugins.template.branding.BrandRoot
import org.jetbrains.plugins.template.branding.brandService

fun copyFileToFolder(event: AnActionEvent, targetFolderName: String) {
    val project: Project = event.project ?: return showErrorLater("Project not found")

    val selectedFile: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        ?: return showErrorLater("No file selected")

    val brandService = project.brandService
    val context = brandService.getBrandContext(selectedFile)
        ?: return showErrorLater("Selected file is not inside a recognized theme root")

    val targetRoot: BrandRoot = brandService.findRootByName(targetFolderName)
        ?: return showErrorLater("Target folder '$targetFolderName' not found in project root")

    if (targetRoot.name == context.currentRoot.name) {
        return showErrorLater("File is already in the target brand")
    }

    // For files only: check if target already exists (folders are always allowed)
    if (!selectedFile.isDirectory) {
        val existing = targetRoot.root.findFileByRelativePath(context.relativePath)
        if (existing != null) {
            return showErrorLater("File already exists in '${targetRoot.displayName}'")
        }
    }

    brandService.copyToBrand(context, targetRoot) { created ->
        // Callback might be called from a coroutine context, wrap VFS access in ReadAction
        // and ensure UI updates happen on EDT
        ReadAction.nonBlocking<Boolean> {
            // Verify that the file actually exists even if created is null
            // (sometimes VfsUtil.copyFile returns null but the file is still created)
            if (!selectedFile.isDirectory) {
                val targetPath = targetRoot.root.findFileByRelativePath(context.relativePath)
                targetPath != null
            } else {
                // For directories, check if the directory exists
                val targetPath = targetRoot.root.findFileByRelativePath(context.relativePath)
                targetPath != null && targetPath.isDirectory
            }
        }.finishOnUiThread(ModalityState.defaultModalityState()) { actuallyExists ->
            // This runs on EDT, safe to show dialogs
            if (created != null || actuallyExists) {
                val itemType = if (selectedFile.isDirectory) "directory" else "file"
                Messages.showInfoMessage(project, "Copied $itemType to ${targetRoot.displayName}", "Success")
            } else {
                Messages.showErrorDialog(project, "Failed to copy to ${targetRoot.displayName}", "Error")
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }
}

private fun showErrorLater(message: String) {
    ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(message, "Error")
    }
}
