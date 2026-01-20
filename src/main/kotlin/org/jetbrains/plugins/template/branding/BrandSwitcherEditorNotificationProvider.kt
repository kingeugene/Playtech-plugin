package org.jetbrains.plugins.template.branding

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.openapi.fileEditor.impl.EditorNotifications
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileEditor.UniqueFileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.EditorEmptyTextPainter
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.FileEditorManagerEx
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.util.function.Function
import javax.swing.BorderFactory
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
                            state.targetFile?.let {
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

