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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form

object DatabaseDialog {

    private val SELECT_DB_WINDOW_WIDTH = 400.dp
    private val SELECT_DB_WINDOW_HEIGHT = 200.dp

    @Composable
    fun SelectDatabase() {
        val selectDBDialog = GlobalState.connection.selectDatabaseDialog
        Dialog(
            title = Label.SELECT_DATABASE,
            onCloseRequest = { selectDBDialog.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(SELECT_DB_WINDOW_WIDTH, SELECT_DB_WINDOW_HEIGHT)
            )
        ) {
            Form.Submission {
                Form.Field(label = Label.SELECT_DATABASE) { DatabaseDropdown(Modifier.fillMaxWidth()) }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    Form.TextButton(text = Label.CLOSE, onClick = { selectDBDialog.close() })
                }
            }
        }
    }

    @Composable
    fun DatabaseDropdown(modifier: Modifier = Modifier) {
        Form.Dropdown(
            values = GlobalState.connection.current?.databaseList ?: emptyList(),
            selected = GlobalState.connection.current?.getDatabase(),
            onExpand = { GlobalState.connection.current?.refreshDatabaseList() },
            onSelection = { GlobalState.connection.current?.openSession(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = GlobalState.connection.isInteractiveMode,
            modifier = modifier
        )
    }
}