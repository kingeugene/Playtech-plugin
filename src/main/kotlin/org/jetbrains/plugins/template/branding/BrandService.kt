package org.jetbrains.plugins.template.branding

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.messages.MessageBusConnection
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import java.util.concurrent.ConcurrentHashMap

enum class BrandRootType {
    CORE,
    INTERLAYER,
    BRAND,
}

data class BrandRoot(
    val name: String,
    val type: BrandRootType,
    val root: VirtualFile,
) {
    val displayName: String = when (type) {
        BrandRootType.CORE -> "Core"
        BrandRootType.INTERLAYER -> "Interlayer"
        BrandRootType.BRAND -> name.removePrefix("app-react-").removeSuffix("-theme")
    }
}

data class BrandState(
    val root: BrandRoot,
    val targetFile: VirtualFile?,
    val isCurrent: Boolean,
)

data class BrandContext(
    val currentRoot: BrandRoot,
    val relativePath: String,
    val sourceFile: VirtualFile,
)

@Service(Service.Level.PROJECT)
class BrandService(private val project: Project) {

    private val logger = Logger.getInstance(BrandService::class.java)

    private val cache = ConcurrentHashMap<String, List<BrandState>>()
    private val connection: MessageBusConnection

    init {
        connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                if (events.any { it.file != null && isThemeRelatedPath(it.file!!) }) {
                    invalidateAll()
                }
            }
        })
    }

    private fun isThemeRelatedPath(file: VirtualFile): Boolean {
        val segments = file.path.split('/', '\\')
        return segments.any { it.startsWith("app-react") }
    }

    fun dispose() {
        connection.dispose()
        cache.clear()
    }

    fun getBrandContext(file: VirtualFile): BrandContext? {
        val roots = getBrandRoots()
        if (roots.isEmpty()) return null

        val currentRoot = roots.firstOrNull { VfsUtilCore.isAncestor(it.root, file, true) } ?: return null

        val relativePath = VfsUtilCore.getRelativePath(file, currentRoot.root, '/')
            ?: return null

        return BrandContext(currentRoot, relativePath, file)
    }

    fun getBrandStates(context: BrandContext): List<BrandState> {
        val key = context.sourceFile.path
        return cache.computeIfAbsent(key) {
            computeBrandStates(context)
        }
    }

    private fun computeBrandStates(context: BrandContext): List<BrandState> {
        val roots = getBrandRoots()
        if (roots.isEmpty()) return emptyList()

        return roots.map { root ->
            val targetFile = root.root.findFileByRelativePath(context.relativePath)
            BrandState(
                root = root,
                targetFile = targetFile,
                isCurrent = root.root == context.currentRoot.root,
            )
        }
    }

    fun getBrandRoots(): List<BrandRoot> {
        val baseDir = project.baseDir ?: return emptyList()

        val candidates = baseDir.children.filter { it.isDirectory && it.name.startsWith("app-react") }

        val ignored = setOf(
            "app-react-example-theme",
            "app-react-licensee",
            "app-react-all-theme",
        )

        val roots = mutableListOf<BrandRoot>()

        candidates.forEach { dir ->
            val name = dir.name
            when {
                name == "app-react" -> roots += BrandRoot(name, BrandRootType.CORE, dir)
                name.contains("interlayer") -> roots += BrandRoot(name, BrandRootType.INTERLAYER, dir)
                name.startsWith("app-react-") && name.endsWith("-theme") && !ignored.contains(name) ->
                    roots += BrandRoot(name, BrandRootType.BRAND, dir)
            }
        }

        val core = roots.filter { it.type == BrandRootType.CORE }
        val interlayer = roots.filter { it.type == BrandRootType.INTERLAYER }
        val brands = roots.filter { it.type == BrandRootType.BRAND }.sortedBy { it.name }

        return core + interlayer + brands
    }

    fun findRootByName(name: String): BrandRoot? =
        getBrandRoots().firstOrNull { it.name == name }

    fun invalidateAll() {
        cache.clear()
    }

    fun invalidateForFile(file: VirtualFile) {
        cache.remove(file.path)
    }

    /**
     * Copy the given source file to the specified brand root, preserving the relative path.
     * Returns the created [VirtualFile] on success, or null on failure.
     */
    fun copyToBrand(context: BrandContext, targetRoot: BrandRoot, onFinished: (VirtualFile?) -> Unit) {
        val relativePath = context.relativePath

        AppExecutorUtil.getAppExecutorService().submit {
            try {
                var createdFile: VirtualFile? = null

                WriteCommandAction.runWriteCommandAction(project) {
                    try {
                        val parentRelativePath = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
                        val targetParent = if (parentRelativePath.isEmpty()) {
                            targetRoot.root
                        } else {
                            VfsUtil.createDirectoryIfMissing(targetRoot.root, parentRelativePath)
                        }

                        createdFile = VfsUtil.copyFile(project, context.sourceFile, targetParent, context.sourceFile.name)
                        invalidateForFile(context.sourceFile)
                        createdFile?.let { invalidateForFile(it) }
                    } catch (e: Exception) {
                        logger.warn("Failed to copy file to brand '${targetRoot.name}'", e)
                        createdFile = null
                    }
                }

                val result = createdFile
                if (result != null) {
                    OpenFileDescriptor(project, result).navigate(true)
                }

                ApplicationManager.getApplication().invokeLater {
                    onFinished(result)
                }
            } catch (e: Exception) {
                logger.warn("Unexpected error while copying file to brand '${targetRoot.name}'", e)
                ApplicationManager.getApplication().invokeLater {
                    onFinished(null)
                }
            }
        }
    }
}

val Project.brandService: BrandService
    get() = this.service()
