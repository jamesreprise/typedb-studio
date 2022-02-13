/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.view.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.common.Property.OS.MACOS
import com.vaticle.typedb.studio.state.project.Directory
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.DIRECTORY
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.FILE
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ComponentSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser
import kotlin.io.path.Path


object ProjectDialog {

    private val OPEN_PROJECT_WINDOW_WIDTH = 500.dp
    private val OPEN_PROJECT_WINDOW_HEIGHT = 140.dp
    private val CREATE_PROJECT_ITEM_WINDOW_WIDTH = 500.dp
    private val CREATE_PROJECT_ITEM_WINDOW_HEIGHT = 200.dp

    private object OpenProjectForm : Form.State {

        var directory: String? by mutableStateOf(GlobalState.project.current?.directory?.absolutePath?.toString())

        override fun isValid(): Boolean {
            return !directory.isNullOrBlank()
        }

        override fun trySubmit() {
            assert(!directory.isNullOrBlank())
            GlobalState.project.tryOpenDirectory(directory!!)
        }
    }

    @Composable
    fun OpenProject() {
        Dialog(
            title = Label.OPEN_PROJECT_DIRECTORY,
            onCloseRequest = { GlobalState.project.openProjectDialog.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(OPEN_PROJECT_WINDOW_WIDTH, OPEN_PROJECT_WINDOW_HEIGHT)
            )
        ) {
            Submission(state = OpenProjectForm) {
                SelectDirectoryField(window)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    OpenProjectButtons()
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryField(window: ComposeDialog) {
        Field(label = Label.DIRECTORY) {
            Row {
                TextInput(
                    value = OpenProjectForm.directory ?: "",
                    placeholder = Label.PATH_TO_PROJECT,
                    onValueChange = { OpenProjectForm.directory = it },
                    modifier = Modifier.fillMaxHeight().weight(1f),
                )
                ComponentSpacer()
                IconButton(icon = Icon.Code.FOLDER_OPEN, onClick = { launchFileDialog(window) })
            }
        }
    }

    private fun launchFileDialog(window: ComposeDialog) {
        when (Property.OS.Current) {
            MACOS -> macOSDialog(window)
            else -> otherOSDialog()
        }
    }

    private fun macOSDialog(window: ComposeDialog) {
        val fileDialog = FileDialog(window, Label.OPEN_PROJECT_DIRECTORY, FileDialog.LOAD).apply {
            file = OpenProjectForm.directory
            isMultipleMode = false
            isVisible = true
        }
        if (fileDialog.directory != null) {
            OpenProjectForm.directory = Path(fileDialog.directory).resolve(fileDialog.file).toString()
        }
    }

    private fun otherOSDialog() {
        val directoryChooser = JFileChooser().apply {
            OpenProjectForm.directory?.let { currentDirectory = File(it) }
            dialogTitle = Label.OPEN_PROJECT_DIRECTORY
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val option = directoryChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val directory = directoryChooser.selectedFile
            assert(directory.isDirectory)
            OpenProjectForm.directory = directory.absolutePath
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun OpenProjectButtons() {
        TextButton(text = Label.CANCEL, onClick = { GlobalState.project.openProjectDialog.close() })
        ComponentSpacer()
        TextButton(text = Label.OPEN, enabled = OpenProjectForm.isValid(), onClick = { OpenProjectForm.trySubmit() })
    }

    private class CreateProjectItemForm(initName: String, val onSubmit: (Directory, String) -> Unit) : Form.State {

        val parent: Directory = GlobalState.project.createItemDialog.parent!!
        var itemName: String by mutableStateOf(initName)

        override fun isValid(): Boolean {
            return itemName.isNotBlank()
        }

        override fun trySubmit() {
            assert(itemName.isNotBlank())
            onSubmit(parent, itemName)
        }
    }

    @Composable
    fun CreateProjectItem() {
        when (GlobalState.project.createItemDialog.type!!) {
            DIRECTORY -> CreateDirectory()
            FILE -> CreateFile()
        }
    }

    @Composable
    private fun CreateDirectory() {
        val form = remember {
            CreateProjectItemForm(GlobalState.project.createItemDialog.parent!!.nexUntitledDirName()) { p, n ->
                GlobalState.project.tryCreateDirectory(p, n)
            }
        }
        CreateProjectItem(
            form = form,
            title = Label.CREATE_DIRECTORY,
            message = Sentence.CREATE_DIRECTORY.format(form.parent)
        )
    }

    @Composable
    private fun CreateFile() {
        val form = remember {
            CreateProjectItemForm(GlobalState.project.createItemDialog.parent!!.nextUntitledFileName()) { p, n ->
                GlobalState.project.tryCreateFile(p, n)
            }
        }
        CreateProjectItem(
            form = form,
            title = Label.CREATE_FILE,
            message = Sentence.CREATE_FILE.format(form.parent)
        )
    }

    @Composable
    private fun CreateProjectItem(form: CreateProjectItemForm, title: String, message: String) {
        Dialog(
            title = title,
            onCloseRequest = { GlobalState.project.createItemDialog.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(CREATE_PROJECT_ITEM_WINDOW_WIDTH, CREATE_PROJECT_ITEM_WINDOW_HEIGHT)
            )
        ) {
            Submission(state = form) {
                Form.Text(value = message, softWrap = true)
                CreateProjectItemField(form.itemName) { form.itemName = it }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    CreateProjectItemButtons(form) { GlobalState.project.createItemDialog.close() }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CreateProjectItemField(text: String, onChange: (String) -> Unit) {
        val focusReq = FocusRequester()
        Field(label = Label.FILE_NAME) {
            TextInput(
                value = text,
                placeholder = "",
                onValueChange = onChange,
                modifier = Modifier.fillMaxHeight().focusRequester(focusReq),
            )
        }
        LaunchedEffect(Unit) { focusReq.requestFocus() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CreateProjectItemButtons(form: Form.State, onCancel: () -> Unit) {
        TextButton(text = Label.CANCEL, onClick = onCancel)
        ComponentSpacer()
        TextButton(text = Label.CREATE, enabled = form.isValid(), onClick = { form.trySubmit() })
    }
}
