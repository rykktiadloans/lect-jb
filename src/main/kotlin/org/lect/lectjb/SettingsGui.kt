package org.lect.lectjb

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunProfileStarter
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.execution.runToolbar.RunToolbarRunProcess
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.RunTab
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.RunState
import com.intellij.execution.util.ExecUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.RunAnythingUtil
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.selected
import com.jetbrains.rd.generator.nova.PredefinedType
import io.ktor.util.*
import org.apache.groovy.util.ScriptRunner
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import java.lang.String.valueOf
import java.lang.annotation.RetentionPolicy
import java.nio.charset.Charset
import javax.swing.*
import javax.swing.text.AbstractDocument.Content


@Service
@State(name = "lectState", storages = [Storage("lectStorage.xml")])
class Settings : SimplePersistentStateComponent<Settings.State>(State()) {
    class State : BaseState() {
        var binaryPath by string("")
        var textPath by string("")
        var codePath by string("")
        var language by string("")
        var outputPath by string("")
        var direction by string("")
        var remove by property(true)
        var suffix by string("")
        var shakeDirection by string("")
        var index by string("")

    }
}

class OpenLect : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if(e.project == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Lect Notification Group")
                .createNotification("Lect Error", "You to open a project for this to work", NotificationType.ERROR)
                .notify(null)
        }
        val index = e.project!!.getService(Settings::class.java).state.index
        if(index!!.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Lect Notification Group")
                .createNotification("Lect Error", "No Lect documentation was generated yet", NotificationType.ERROR)
                .notify(null)
        }
        BrowserUtil.browse("file://$index")
    }

}

class SettingsGuiWrapper(private val project: Project) : DialogWrapper(true) {
    var binaryPath: String = ""
    var textPath: String = ""
    var codePath: String = ""
    var language: String = ""
    var outputPath = ""
    var direction = "Up-to-down"
    var remove = true
    var suffix = ""
    var shakeDirection = "Roots"

    init {
        title = "Lect Settings"

        val settings = project.getService(Settings::class.java)
        this.binaryPath = settings.state.binaryPath.toString()
        this.textPath = settings.state.textPath.toString()
        this.codePath = settings.state.codePath.toString()
        this.language = settings.state.language.toString()
        this.outputPath = settings.state.outputPath.toString()
        this.direction = settings.state.direction.toString()
        this.remove = settings.state.remove
        this.suffix = settings.state.suffix.toString()
        this.shakeDirection = settings.state.shakeDirection.toString()
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Lect Binary") {
                textFieldWithBrowseButton("Lect Binary File",
                    project,
                    FileChooserDescriptor(true, false,
                        false, false,
                        false, false))

                    .bindText(this@SettingsGuiWrapper::binaryPath)
                    .validationOnApply {
                        if(it.text.trim().isEmpty()) {
                            return@validationOnApply error("Lect Binary should point to Lect executable")
                        }
                        return@validationOnApply null
                    }
            }
            row("Text annotations") {
                textFieldWithBrowseButton("Text Annotation Directory",
                    project,
                    FileChooserDescriptor(false, true,
                        false, false,
                        false, false))

                    .bindText(this@SettingsGuiWrapper::textPath)
                    .validationOnApply {
                        if(it.text.trim().isEmpty()) {
                            return@validationOnApply error("Text Annotation Directory must point to a directory")
                        }
                        return@validationOnApply null
                    }
            }
            row("Project code") {
                textFieldWithBrowseButton("Code Directory",
                    project,
                    FileChooserDescriptor(true, true,
                        false, false,
                        false, false))
                    .bindText(this@SettingsGuiWrapper::codePath)
                    .validationOnApply {
                        if(it.text.trim().isEmpty()) {
                            return@validationOnApply error("Code Directory must point to a directory")
                        }
                        return@validationOnApply null
                    }
            }
            row("Language") {
                comboBox(listOf("", "C++", "Java"))
                    .bindItem({this@SettingsGuiWrapper.language}, {this@SettingsGuiWrapper.language = it ?: "" })
                    .validationOnApply {
                        if(it.selectedItem!!.toString().trim().isEmpty()) {
                            return@validationOnApply error("You have to choose a language")
                        }
                        return@validationOnApply null
                    }

            }
            row("Output directory") {
                textFieldWithBrowseButton("Output Directory",
                    project,
                    FileChooserDescriptor(false, true,
                        false, false,
                        false, false))
                    .bindText(this@SettingsGuiWrapper::outputPath)
                    .validationOnApply {
                        if(it.text.trim().isEmpty()) {
                            return@validationOnApply error("Output Directory must point to a directory")
                        }
                        return@validationOnApply null
                    }
            }

            separator().rowComment("Optional")

            row("Tree direction") {
                comboBox(listOf("Up-to-down", "Down-to-up", "Right-to-left", "Left-to-right"))
                    .bindItem({this@SettingsGuiWrapper.direction}, {this@SettingsGuiWrapper.direction = it ?: "" })
                    .validationOnApply {
                        if(it.selectedItem!!.toString().trim().isEmpty()) {
                            return@validationOnApply error("You have to choose a direction")
                        }
                        return@validationOnApply null
                    }
            }
            row("Reduce content of code annotations") {
                checkBox("Reduce content of code annotations")
                    .bindSelected(this@SettingsGuiWrapper::remove)
            }

            row("Mandatory code annotation suffix (leave empty if none)") {
                textField()
                    //.comment("Mandatory code annotation suffix (leave empty if none)")
                    .bindText(this@SettingsGuiWrapper::suffix)
            }

            row("Node shake direction") {
                comboBox(listOf("Leaves", "Roots"))
                    .bindItem({this@SettingsGuiWrapper.shakeDirection}, {this@SettingsGuiWrapper.shakeDirection = it ?: "" })
                    .validationOnApply {
                        if(it.selectedItem!!.toString().trim().isEmpty()) {
                            return@validationOnApply error("You have to choose a shake direction")
                        }
                        return@validationOnApply null
                    }
            }

        }
    }

    override fun doOKAction() {
        super.applyFields()
        val settings = project.getService(Settings::class.java)
        settings.state.binaryPath = this.binaryPath
        settings.state.textPath = this.textPath
        settings.state.codePath = this.codePath
        settings.state.language = this.language
        settings.state.outputPath = this.outputPath
        settings.state.direction = this.direction
        settings.state.remove = this.remove
        settings.state.suffix = this.suffix
        settings.state.shakeDirection = this.shakeDirection

        val languages = mapOf(Pair("C++", "c++"), Pair("Java", "java"))
        val language: String = languages[this.language] ?: ""
        if(language.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Lect Notification Group")
                .createNotification("Lect Error",
                    "It seems that no language was supplied", NotificationType.ERROR)
                .notify(this.project)
            return
        }

        val directions = mapOf(Pair("Up-to-down", "UD"), Pair("Down-to-up", "DU"), Pair("Right-to-left", "RL"), Pair("Left-to-right", "LR"))
        val direction: String = directions[this.direction] ?: ""
        if(direction.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Lect Notification Group")
                .createNotification("Lect Error",
                    "`${this.direction}` is not a valid direction`", NotificationType.ERROR)
                .notify(this.project)
            return
        }

        val shakeDirections = mapOf(Pair("Leaves", "leaves"), Pair("Roots", "roots"))
        val shakeDirection: String = shakeDirections[this.shakeDirection] ?: ""
        if(shakeDirection.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Lect Notification Group")
                .createNotification("Lect Error",
                    "It seems that no shake direction was supplied", NotificationType.ERROR)
                .notify(this.project)
            return
        }



        val commandLine = GeneralCommandLine()
        commandLine.exePath = this.binaryPath
        commandLine.addParameters("-t", this.textPath)
        commandLine.addParameters("-s", this.codePath)
        commandLine.addParameters("-l", language)
        commandLine.addParameters("-o", this.outputPath)
        commandLine.addParameters("-d", direction)
        commandLine.addParameters("-lup", shakeDirection)
        commandLine.addParameters("-jb")
        if(this.remove) {
            commandLine.addParameter("-r")
        }
        if(this.suffix.trim().isNotEmpty()) {
            commandLine.addParameters("-suf", this.suffix)
        }
        commandLine.charset = Charset.forName("UTF-8")
        commandLine.workDirectory = File(project.basePath!!)
        try {
            val runProfile = CmdRunProfile(commandLine)
            ExecutionEnvironmentBuilder.create(project, DefaultRunExecutor(), runProfile)
                .buildAndExecute()
        }
        catch (e: ExecutionException) {
            System.err.println(e.message)
        }
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Lect Notification Group")
            .createNotification("Lect Info",
                "Lect worked successfully!", NotificationType.INFORMATION)
            .notify(this.project)

        val separator = if (outputPath[outputPath.length - 1] == '/') "" else "/"
        settings.state.index = "$outputPath$separator" + "index.html"


        super.doOKAction()
    }
}

class CmdRunState(environment: ExecutionEnvironment) : CommandLineState(environment) {
    private fun getRunProfile(): CmdRunProfile {
        val runProfile = environment.runProfile
        if(runProfile !is CmdRunProfile) {
            throw IllegalStateException("Got $runProfile instead of RunAnything profile");
        }
        return runProfile
    }
    override fun startProcess(): ProcessHandler {
        val runProfile = this.getRunProfile()
        val commandLine = runProfile.commandLine
        val processHandler = KillableColoredProcessHandler(commandLine)
        return processHandler
    }

}

class CmdRunProfile(val commandLine: GeneralCommandLine) : RunProfile {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return CmdRunState(environment)
    }

    override fun getName(): String {
        return "Lect Profile"
    }

    override fun getIcon(): Icon? {
        return AllIcons.Actions.Run_anything
    }

}

class OpenSettingsGui : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if(e.project == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Lect Notification Group")
                .createNotification("Lect Error", "You to open a project for this to work", NotificationType.ERROR)
                .notify(null)
            return
        }
        val project = e.project!!

        val gui = SettingsGuiWrapper(project)
        gui.showAndGet()


    }

}