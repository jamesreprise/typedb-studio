/*
 * Copyright (C) 2022 Vaticle
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
 *
 */

package com.vaticle.typedb.studio.module.user

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Sentence


object ChangeDefaultPasswordDialog {

    private val WIDTH = 500.dp
    private val HEIGHT = 225.dp

    private val state by mutableStateOf(ChangeInitialPasswordForm())

    private class ChangeInitialPasswordForm : Form.State() {
        var oldPassword: String by mutableStateOf("")
        var newPassword: String by mutableStateOf("")
        var repeatPassword: String by mutableStateOf("")

        override fun cancel() {

        }

        override fun isValid() = oldPassword.isNotEmpty()
                    && newPassword.isNotEmpty()
                    && oldPassword != newPassword
                    && repeatPassword == newPassword

        override fun submit() {
            Service.driver.tryUpdateUserPassword(state.oldPassword, state.newPassword)
            Service.driver.close()
            Service.driver.connectServerDialog.open()
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.driver.changeInitialPasswordDialog.isOpen) ChangeInitialPassword()
    }

    @Composable
    private fun ChangeInitialPassword() = Dialog.Layout(
        state = Service.driver.connectServerDialog,
        title = Sentence.CHANGE_DEFAULT_PASSWORD_FOR_USER.format(Service.driver.username).removeSuffix("."),
        width = WIDTH,
        height = HEIGHT
    ) {
        Form.Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = true) {
            OldPasswordFormField(state)
            NewPasswordFormField(state)
            RepeatPasswordFormField(state)
        }
    }

    @Composable
    private fun OldPasswordFormField(state: ChangeInitialPasswordForm) = Form.Field(label = Label.OLD_PASSWORD) {
        Form.TextInput(
            value = state.oldPassword,
            onValueChange = { state.oldPassword = it },
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun NewPasswordFormField(state: ChangeInitialPasswordForm) = Form.Field(label = Label.NEW_PASSWORD) {
        Form.TextInput(
            value = state.newPassword,
            onValueChange = { state.newPassword = it },
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun RepeatPasswordFormField(state: ChangeInitialPasswordForm) = Form.Field(label = Label.REPEAT_PASSWORD) {
        Form.TextInput(
            value = state.repeatPassword,
            onValueChange = { state.repeatPassword = it },
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}