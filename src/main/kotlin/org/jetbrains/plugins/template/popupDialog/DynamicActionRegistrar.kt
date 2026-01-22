package org.jetbrains.plugins.template.popupDialog

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.template.branding.brandService
import java.util.Locale.getDefault

@Service
class DynamicActionRegistrar {

    private val logger = Logger.getInstance(DynamicActionRegistrar::class.java)

    fun registerActions(project: Project) {
        // Use BrandService to get roots in the same order (CORE, INTERLAYER, BRAND sorted)
        val brandService = project.brandService
        val brandRoots = brandService.getBrandRoots()

        // Register actions dynamically
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction("CopyFileToGroup") as DefaultActionGroup

        brandRoots.forEach { brandRoot ->
            val folderName = brandRoot.name
            val actionText = brandRoot.displayName

            // Create the action for this folder
            val action = CopyToDynamicFolderAction(actionText, folderName)
            val actionId = "CopyTo${actionText.replace(" ", "")}Action"

            // Register the action
            if (actionManager.getAction(actionId) == null) {
                actionManager.registerAction(actionId, action)
                group.add(action)
                logger.info("Registered action for folder: $folderName as $actionId")
            }
        }
    }
}
