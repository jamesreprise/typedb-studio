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

package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.theme.Color.fadeable
import com.vaticle.typedb.studio.common.theme.Theme
import java.awt.event.KeyEvent.KEY_RELEASED

object Form {

    val SPACING = 16.dp

    private const val LABEL_WEIGHT = 1f
    private const val INPUT_WEIGHT = 3f
    private val FIELD_SPACING = 12.dp
    private val FIELD_HEIGHT = 28.dp
    private val BORDER_WIDTH = 1.dp
    private val CONTENT_PADDING = 8.dp
    private val ICON_SPACING = 4.dp
    private val ROUNDED_RECTANGLE = RoundedCornerShape(4.dp)

    private val RowScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT)
    private val RowScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    @Composable
    fun Field(label: String, fieldInput: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value = label, modifier = LABEL_MODIFIER)
            Column(modifier = INPUT_MODIFIER) { fieldInput() }
        }
    }

    @Composable
    fun FieldGroup(content: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)) {
            content()
        }
    }

    @Composable
    fun Text(
        value: String,
        style: TextStyle = Theme.typography.body1,
        color: Color = Theme.colors.onPrimary,
        modifier: Modifier = Modifier
    ) {
        androidx.compose.material.Text(text = value, style = style, color = color, modifier = modifier)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        color: Color = Theme.colors.onPrimary,
        enabled: Boolean = true
    ) {
        BoxButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Text(text, style = Theme.typography.body1, color = fadeable(color, !enabled))
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun IconButton(
        icon: Icon.Code,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        color: Color = Theme.colors.onPrimary,
        enabled: Boolean = true
    ) {
        BoxButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Icon.Render(icon = icon, color = color)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun BoxButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable BoxScope.() -> Unit
    ) {
        val focusManager = LocalFocusManager.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .height(FIELD_HEIGHT)
                .background(fadeable(Theme.colors.primary, !enabled), ROUNDED_RECTANGLE)
                .padding(horizontal = CONTENT_PADDING)
                .focusable(enabled = enabled)
                .pointerIcon(icon = PointerIcon.Hand) // TODO: #516
                .clickable(enabled = enabled) { onClick() }
                .onKeyEvent { onKeyEvent(it, focusManager, onClick, enabled) }
        ) {
            content()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextSelectable(
        value: String,
        style: TextStyle = Theme.typography.body1,
        color: Color = Theme.colors.onSurface,
        modifier: Modifier = Modifier
    ) {
        val focusManager = LocalFocusManager.current
        BasicTextField(
            modifier = modifier
                .pointerIcon(PointerIcon.Text) // TODO: #516
                .onPreviewKeyEvent { onKeyEvent(it, focusManager) },
            value = value,
            onValueChange = {},
            readOnly = true,
            cursorBrush = SolidColor(Theme.colors.secondary),
            textStyle = style.copy(color = color)
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextInput(
        value: String,
        placeholder: String,
        onValueChange: (String) -> Unit,
        maxLines: Int = 1,
        singleLine: Boolean = true,
        readOnly: Boolean = false,
        enabled: Boolean = true,
        isPassword: Boolean = false,
        modifier: Modifier = Modifier,
        textStyle: TextStyle = Theme.typography.body1,
        pointerHoverIcon: PointerIcon = PointerIcon.Text, // TODO: #516 PointerIconDefaults.Text
        trailingIcon: (@Composable () -> Unit)? = null,
        leadingIcon: (@Composable () -> Unit)? = null
    ) {
        val focusManager = LocalFocusManager.current // for @Composable to be called in lambda
        val borderBrush = SolidColor(fadeable(Theme.colors.surface2, !enabled))
        BasicTextField(
            modifier = modifier
                .background(fadeable(Theme.colors.surface, !enabled), ROUNDED_RECTANGLE)
                .border(width = BORDER_WIDTH, brush = borderBrush, shape = ROUNDED_RECTANGLE)
                .pointerIcon(pointerHoverIcon) // TODO: #516
                .onPreviewKeyEvent { onKeyEvent(event = it, focusManager = focusManager, enabled = enabled) },
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            cursorBrush = SolidColor(Theme.colors.secondary),
            textStyle = textStyle.copy(color = fadeable(Theme.colors.onSurface, !enabled)),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                Row(modifier.padding(horizontal = ICON_SPACING), verticalAlignment = Alignment.CenterVertically) {
                    leadingIcon?.let { leadingIcon(); Spacer(Modifier.width(ICON_SPACING)) }
                    Box(modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                        if (value.isEmpty()) Text(value = placeholder, color = fadeable(Theme.colors.onSurface, true))
                    }
                    trailingIcon?.let { Spacer(Modifier.width(ICON_SPACING)); trailingIcon() }
                }
            },
        )
    }

    @Composable
    fun Checkbox(
        checked: Boolean,
        onChange: ((Boolean) -> Unit)?,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            modifier = modifier.size(FIELD_HEIGHT)
                .background(color = fadeable(Theme.colors.surface, !enabled))
                .border(BORDER_WIDTH, SolidColor(fadeable(Theme.colors.surface2, !enabled)), ROUNDED_RECTANGLE),
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = fadeable(Theme.colors.icon, !enabled),
                uncheckedColor = fadeable(Theme.colors.surface, !enabled),
                disabledColor = fadeable(Theme.colors.surface, !enabled)
            )
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Dropdown(
        values: List<T>,
        selected: T,
        onSelection: (value: T) -> Unit,
        placeholder: String = "",
        enabled: Boolean = true,
        dropdownMaxHeight: Dp? = null,
        modifier: Modifier = Modifier,
        textInputModifier: Modifier = Modifier,
        textStyle: TextStyle = Theme.typography.body1,
        leadingIcon: (@Composable () -> Unit)? = null
    ) {

        val dropdownIcon: @Composable () -> Unit = { Icon.Render(icon = Icon.Code.CaretDown, enabled = enabled) }
        val focusManager = LocalFocusManager.current // for @Composable to be called in lambda
        val focusRequester = FocusRequester()

        class DropdownState {
            var expanded by mutableStateOf(false)
            var mouseIndex: Int? by mutableStateOf(null)

            fun toggle() { expanded = !expanded }
            fun select(value: T) { onSelection(value); expanded = false }
            fun mouseOutFrom(index: Int): Boolean { if (mouseIndex == index) mouseIndex = null; return false }
            fun mouseInTo(index: Int): Boolean { mouseIndex = index; return true }
        }

        val state = remember { DropdownState() }
        var dropdownModifier = Modifier.background(Theme.colors.surface)
        if (dropdownMaxHeight != null) dropdownModifier = dropdownModifier.requiredHeightIn(max = dropdownMaxHeight)
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            TextInput(
                value = selected.toString(), onValueChange = {}, readOnly = true, placeholder = placeholder,
                enabled = enabled, textStyle = textStyle, leadingIcon = leadingIcon, trailingIcon = dropdownIcon,
                pointerHoverIcon = PointerIcon.Hand, modifier = textInputModifier
                    .fillMaxSize().focusable(enabled = enabled).focusRequester(focusRequester)
                    .clickable(enabled = enabled) { state.toggle(); focusRequester.requestFocus() }
                    .onKeyEvent { onKeyEvent(it, focusManager) }
            )
            DropdownMenu(
                expanded = state.expanded,
                onDismissRequest = { state.expanded = false },
                modifier = dropdownModifier
            ) {
                val padding = PaddingValues(horizontal = CONTENT_PADDING)
                val itemModifier = Modifier.height(FIELD_HEIGHT)
                if (values.isEmpty()) DropdownMenuItem(
                    onClick = {}, contentPadding = padding,
                    modifier = itemModifier.background(Theme.colors.surface)
                ) {
                    Text(value = "(${Label.NONE})", style = textStyle)
                } else values.forEachIndexed { i, value ->
                    DropdownMenuItem(
                        onClick = { state.select(value) }, contentPadding = padding, modifier = itemModifier
                            .background(if (i == state.mouseIndex) Theme.colors.primary else Theme.colors.surface)
                            .pointerMoveFilter(onExit = { state.mouseOutFrom(i) }, onEnter = { state.mouseInTo(i) })
                            .pointerIcon(PointerIcon.Hand)
                    ) {
                        val isSelected = value == selected
                        val color = if (isSelected) Theme.colors.secondary else Theme.colors.onSurface
                        Text(value = value.toString(), style = textStyle, color = color)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun onKeyEvent(
        event: KeyEvent, focusManager: FocusManager, onClick: (() -> Unit)? = null, enabled: Boolean = true
    ): Boolean {
        return when {
            event.nativeKeyEvent.id == KEY_RELEASED -> false
            !enabled -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    onClick?.let { onClick(); true } ?: false
                }
                Key.Tab -> {
                    focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Up else FocusDirection.Down); true
                }
                else -> false
            }
        }
    }
}