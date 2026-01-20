package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.diagnostic.Logger
import java.util.Locale.getDefault

@Service
class DynamicActionRegistrar {

    private val logger = Logger.getInstance(DynamicActionRegistrar::class.java)

    // List of folders to ignore
    private val ignoredFolders = listOf(
        "app-react-example-theme",
        "app-react-licensee",
        "app-react-all-theme",
    )

    fun registerActions(project: Project) {
        // Get the base directory of the project
        val baseDir = project.basePath ?: return
        val virtualBaseDir = LocalFileSystem.getInstance().findFileByPath(baseDir) ?: return

        // Find all directories containing "app-react-" and not in ignoredFolders
        val targetFolders = virtualBaseDir.children.filter {
            it.isDirectory &&
                    it.name.contains("app-react-") &&
                    !ignoredFolders.contains(it.name)
        }

        // Register actions dynamically
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction("CopyFileToGroup") as DefaultActionGroup

        targetFolders.forEach { folder ->
            val folderName = folder.name
            val actionText = folderName.removePrefix("app-react-").replace("-theme", "")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }

            // Create the action for this folder
            val action = CopyToDynamicFolderAction(actionText, folderName)
            val actionId = "CopyTo${actionText}Action"

            // Register the action
            if (actionManager.getAction(actionId) == null) {
                actionManager.registerAction(actionId, action)
                group.add(action)
                logger.info("Registered action for folder: $folderName as $actionId")
            }
        }
    }
}
