package org.jetbrains.plugins.template.branding

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.util.function.Function
import javax.swing.JComponent
import javax.swing.JPanel

class BrandSwitcherEditorNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
        val brandService = project.service<BrandService>()
        val context = brandService.getBrandContext(file) ?: return null

        return Function<FileEditor, JComponent?> { _ ->
            createComponent(project, context)
        }
    }

    private fun createComponent(project: Project, context: BrandContext): JComponent {
        val brandService = project.service<BrandService>()
        val states = brandService.getBrandStates(context)
        if (states.isEmpty()) return JPanel() // should not happen but keep safe

        val panel = NonOpaquePanel(BorderLayout())
        panel.border = JBUI.Borders.empty(2, 4)

        val iconsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        iconsPanel.isOpaque = false

        states.forEach { state ->
            val label = JBLabel(state.root.displayName)
            label.border = JBUI.Borders.empty(0, 4)
            label.isOpaque = false

            when {
                state.isCurrent -> {
                    label.toolTipText = "Current brand"
                    label.cursor = Cursor.getDefaultCursor()
                    label.foreground = JBColor.BLUE
                }

                state.targetFile != null -> {
                    label.toolTipText = "Open in ${state.root.displayName}"
                    label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    label.foreground = JBColor.foreground()

                    label.addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                            state.targetFile.let {
                                com.intellij.openapi.fileEditor.OpenFileDescriptor(project, it).navigate(true)
                            }
                        }
                    })
                }

                else -> {
                    label.toolTipText = "Copy current file to ${state.root.displayName}"
                    label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    label.foreground = JBColor.GRAY

                    label.addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                            val answer = Messages.showYesNoDialog(
                                project,
                                "Do you want to copy the current file to ${state.root.displayName}?",
                                "File not found in ${state.root.displayName}",
                                "Copy",
                                "Cancel",
                                null,
                            )

                            if (answer == Messages.YES) {
                                brandService.copyToBrand(context, state.root) { created ->
                                    EditorNotifications.getInstance(project).updateNotifications(context.sourceFile)
                                    created?.let {
                                        EditorNotifications.getInstance(project).updateNotifications(it)
                                    }
                                }
                            }
                        }
                    })
                }
            }

            iconsPanel.add(label)
        }

        panel.add(iconsPanel, BorderLayout.EAST)
        return panel
    }
}

