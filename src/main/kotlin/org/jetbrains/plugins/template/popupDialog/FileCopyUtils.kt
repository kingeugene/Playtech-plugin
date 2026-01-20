package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.application.ApplicationManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

fun copyFileToFolder(event: AnActionEvent, targetFolderName: String) {
    // Get the current project
    val project: Project? = event.project
    if (project == null) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("Project not found", "Error")
        }
        return
    }

    // Get the selected file or directory
    val selectedFile: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)
    if (selectedFile == null) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("No file or directory selected", "Error")
        }
        return
    }

    // Get the path to the folder where the selected file or directory is located
    val sourceThemeFolder = selectedFile.path.split("/").find { it.startsWith("app-react") }
    if (sourceThemeFolder == null) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("Selected file is not inside a theme folder", "Error")
        }
        return
    }

    // Path to the project root
    val projectBaseDir = project.basePath?.let {
        com.intellij.openapi.vfs.VfsUtil.findFile(Paths.get(it), true)
    }

    if (projectBaseDir == null) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("Project base directory not found", "Error")
        }
        return
    }

    // Find the target folder in the project root
    val targetFolder: VirtualFile? = projectBaseDir.findChild(targetFolderName)
    if (targetFolder == null || !targetFolder.isDirectory) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("Target folder '$targetFolderName' not found in project root", "Error")
        }
        return
    }

    // Determine the relative path of the selected file/directory relative to the source folder
    val relativePath = selectedFile.path.substringAfter(sourceThemeFolder)

    // Determine the path where the file or directory will be copied in the target folder
    val targetFilePath = Paths.get(targetFolder.path, relativePath)

    // Perform the copy inside WriteCommandAction
    WriteCommandAction.runWriteCommandAction(project) {
        try {
            if (selectedFile.isDirectory) {
                // If it's a directory, copy it recursively
                copyDirectoryRecursively(Paths.get(selectedFile.path), targetFilePath)
            } else {
                // If it's a file, copy it
                Files.createDirectories(targetFilePath.parent)
                Files.copy(selectedFile.inputStream, targetFilePath)
            }

            ApplicationManager.getApplication().invokeLater {
                Messages.showInfoMessage("Copied to '$targetFilePath'", "Success")
            }

        } catch (e: Exception) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog("Failed to copy: ${e.message}", "Error")
            }
        }
    }
}

fun copyDirectoryRecursively(source: Path, target: Path) {
    Files.walk(source).forEach { path ->
        val targetPath = target.resolve(source.relativize(path))
        if (Files.isDirectory(path)) {
            targetPath.createDirectories()
        } else {
            Files.copy(path, targetPath)
        }
    }
}
