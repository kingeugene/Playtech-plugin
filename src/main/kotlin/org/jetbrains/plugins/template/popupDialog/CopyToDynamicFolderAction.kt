package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CopyToDynamicFolderAction(folderLabel: String, private val folderName: String) : AnAction("Copy to $folderLabel") {
    override fun actionPerformed(event: AnActionEvent) {
        copyFileToFolder(event, folderName)
    }
}
