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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Message.View.Companion.EXPAND_LIMIT_REACHED
import com.vaticle.typedb.studio.state.common.Message.View.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.view.common.component.Form.IconArgs
import com.vaticle.typedb.studio.view.common.component.Navigator.ItemState.Expandable
import com.vaticle.typedb.studio.view.common.component.Navigator.ItemState.Expandable.Container
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.INDICATION_HOVER_ALPHA
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.LinkedList
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

object Navigator {

    @OptIn(ExperimentalTime::class)
    private val LIVE_UPDATE_REFRESH_RATE = Duration.seconds(3)
    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp
    private const val MAX_ITEM_EXPANDED = 5000
    private const val SCROLL_ITEM_OFFSET = 3
    private val LOGGER = KotlinLogging.logger {}

    open class ItemState<T : Navigable.Item<T>> internal constructor(
        open val item: T, val parent: Expandable<T>?
    ) : Comparable<ItemState<T>> {

        open val isExpandable: Boolean = false
        val name get() = item.name
        val info get() = item.info
        var focusReq: FocusRequester? = null
        var next: ItemState<T>? by mutableStateOf(null)
        var previous: ItemState<T>? by mutableStateOf(null)
        var index: Int by mutableStateOf(0)
        var depth: Int by mutableStateOf(0)

        open fun asExpandable(): Expandable<T> {
            throw TypeCastException(ILLEGAL_CAST.message(ItemState::class.simpleName, Expandable::class.simpleName))
        }

        override fun compareTo(other: ItemState<T>): Int {
            return item.compareTo(other.item)
        }

        internal open fun navigables(depth: Int): List<ItemState<T>> {
            return listOf(this)
        }

        open class Expandable<T : Navigable.Item<T>> internal constructor(
            expandable: Navigable.ExpandableItem<T>, parent: Expandable<T>?, private val navState: NavigatorState<T>
        ) : ItemState<T>(expandable as T, parent) {

            override val isExpandable: Boolean = true
            var isExpanded: Boolean by mutableStateOf(false)
            var entries: List<ItemState<T>> by mutableStateOf(emptyList())

            override fun asExpandable(): Expandable<T> {
                return this
            }

            override fun navigables(depth: Int): List<ItemState<T>> {
                val list: MutableList<ItemState<T>> = mutableListOf(this)
                if (isExpanded) list.addAll(navigableChildren(depth + 1))
                return list
            }

            protected fun navigableChildren(depth: Int): List<ItemState<T>> {
                return entries.onEach { it.depth = depth }.map { it.navigables(depth) }.flatten()
            }

            fun toggle() {
                if (isExpanded) collapse()
                else expand()
            }

            fun collapse(recomputeNavigator: Boolean = true) {
                isExpanded = false
                if (recomputeNavigator) navState.recomputeList()
            }

            fun expand(recomputeNavigator: Boolean = true) {
                expand(recomputeNavigator, 1)
            }

            internal fun expand(depth: Int) {
                expand(true, depth)
            }

            internal fun expand(recomputeNavigator: Boolean, depth: Int) {
                expand(1, depth)
                if (recomputeNavigator) navState.recomputeList()
            }

            private fun expand(currentDepth: Int, maxDepth: Int) {
                isExpanded = true
                reloadEntries()
                if (currentDepth < maxDepth) {
                    entries.filterIsInstance<Expandable<T>>().forEach { it.expand(currentDepth + 1, maxDepth) }
                }
            }

            private fun reloadEntries() {
                item.asExpandable().reloadEntries()
                val new = item.asExpandable().entries.toSet()
                val old = entries.map { it.item }.toSet()
                if (new != old) {
                    val deleted = old - new
                    val added = new - old
                    val updatedEntries = entries.filter { !deleted.contains(it.item) } +
                            added.map { itemStateOf(it) }.toList()
                    entries = updatedEntries.sorted()
                }
                entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded }.forEach { it.reloadEntries() }
            }

            internal fun checkForUpdate() {
                if (!isExpanded) return
                item.asExpandable().reloadEntries()
                if (item.asExpandable().entries.toSet() != entries.map { it.item }.toSet()) reloadEntries()
                entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded }.forEach { it.checkForUpdate() }
            }

            internal open fun itemStateOf(item: T): ItemState<T> {
                return if (item.isExpandable) Expandable(item.asExpandable(), this, navState)
                else ItemState(item, this)
            }

            internal class Container<T : Navigable.Item<T>> internal constructor(
                expandable: Navigable.ExpandableItem<T>, val navState: NavigatorState<T>
            ) : Expandable<T>(expandable, null, navState) {

                fun navigables(): List<ItemState<T>> {
                    return navigableChildren(0)
                }

                override fun itemStateOf(item: T): ItemState<T> {
                    return if (item.isExpandable) Expandable(item.asExpandable(), null, navState)
                    else ItemState(item, this)
                }
            }
        }
    }

    class NavigatorState<T : Navigable.Item<T>> internal constructor(
        container: Navigable.Container<T>,
        private val title: String,
        private val initExpandDepth: Int,
        private val liveUpdate: Boolean,
        private val openFn: (ItemState<T>) -> Unit,
        private var coroutineScope: CoroutineScope
    ) {
        private var container: Container<T> by mutableStateOf(Container(container, this)); private set
        internal var entries: List<ItemState<T>> by mutableStateOf(emptyList()); private set
        internal var minWidth by mutableStateOf(0.dp); private set
        internal var viewState: LazyListState? by mutableStateOf(null)
        internal var selected: ItemState<T>? by mutableStateOf(null); private set
        internal var hovered: ItemState<T>? by mutableStateOf(null)
        val buttons: List<Form.ButtonArgs> = listOf(
            Form.ButtonArgs(Icon.Code.CHEVRONS_DOWN) { expand() },
            Form.ButtonArgs(Icon.Code.CHEVRONS_UP) { collapse() }
        )

        init {
            initialiseContainer()
        }

        private fun initialiseContainer() {
            container.expand(false, 1 + initExpandDepth)
            if (liveUpdate) initWatcher(container)
            recomputeList()
        }

        fun replaceContainer(newContainer: Navigable.Container<T>) {
            container = Container(newContainer, this)
            initialiseContainer()
        }

        @OptIn(ExperimentalTime::class)
        private fun initWatcher(root: Container<T>) {
            coroutineScope.launch {
                try {
                    do {
                        delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
                        root.checkForUpdate()
                    } while (root == container)
                } catch (e: CancellationException) {
                } catch (e: java.lang.Exception) {
                    State.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
                }
            }
        }

        private fun expand() {
            var i = 0
            val queue = LinkedList(container.entries.filterIsInstance<Expandable<T>>())
            while (queue.isNotEmpty() && i < MAX_ITEM_EXPANDED) {
                val item = queue.pop()
                item.expand(false)
                i += 1 + item.entries.count { !it.isExpandable }
                queue.addAll(item.entries.filterIsInstance<Expandable<T>>())
            }
            recomputeList()
            if (!queue.isEmpty()) {
                State.notification.userWarning(LOGGER, EXPAND_LIMIT_REACHED, title, MAX_ITEM_EXPANDED)
            }
        }

        private fun collapse() {
            val queue = LinkedList(container.entries.filterIsInstance<Expandable<T>>())
            while (queue.isNotEmpty()) {
                val item = queue.pop()
                item.collapse(false)
                queue.addAll(item.entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded })
            }
            recomputeList()
        }

        internal fun recomputeList() {
            var previous: ItemState<T>? = null
            entries = container.navigables().onEachIndexed { i, item ->
                previous?.let { it.next = item }
                item.previous = previous
                item.index = i
                previous = item
            }
            previous?.next = null
        }

        internal fun mayIncreaseMinWidth(width: Dp) {
            if (width > minWidth) minWidth = width
        }

        internal fun open(item: ItemState<T>) {
            openFn(item)
        }

        internal fun selectNext(item: ItemState<T>) {
            item.next?.let { select(it) }
        }

        internal fun selectPrevious(item: ItemState<T>) {
            item.previous?.let { select(it) }
        }

        internal fun selectParent(item: ItemState<T>) {
            item.parent?.let { select(it) }
        }

        internal fun select(item: ItemState<T>) {
            selected = item
            mayScrollToAndFocusOnSelected()
        }

        private fun mayScrollToAndFocusOnSelected() {
            var scrollTo = -1
            val layout = viewState!!.layoutInfo
            if (layout.visibleItemsInfo.isNotEmpty()) {
                val visible = max(layout.visibleItemsInfo.size - 1, 1)
                val offset = min(SCROLL_ITEM_OFFSET, floor(visible / 2.0).toInt())
                val firstInc = viewState!!.firstVisibleItemIndex
                val startInc = firstInc + offset
                val lastExc = firstInc + visible
                val endExc = lastExc - offset
                val target = selected!!.index

                if (target < startInc) scrollTo = max(target - offset, 0)
                else if (target >= endExc) scrollTo = min(target + offset + 1, layout.totalItemsCount) - visible
            }

            if (scrollTo >= 0) coroutineScope.launch {
                if (scrollTo >= 0) viewState!!.animateScrollToItem(scrollTo)
                selected!!.focusReq?.requestFocus()
            } else selected!!.focusReq?.requestFocus()
        }
    }

    @Composable
    fun <T : Navigable.Item<T>> rememberNavigatorState(
        container: Navigable.Container<T>, title: String, initExpandDepth: Int,
        liveUpdate: Boolean, openFn: (ItemState<T>) -> Unit
    ): NavigatorState<T> {
        val coroutineScope = rememberCoroutineScope()
        return remember { NavigatorState(container, title, initExpandDepth, liveUpdate, openFn, coroutineScope) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Navigable.Item<T>> Layout(
        navState: NavigatorState<T>,
        iconArgs: (ItemState<T>) -> IconArgs,
        contextMenuFn: (ItemState<T>) -> List<ContextMenu.Item>
    ) {
        val density = LocalDensity.current.density
        val ctmState = ContextMenu.rememberState()
        val lazyListState = rememberLazyListState()
        navState.viewState = lazyListState
        Box(modifier = Modifier.fillMaxSize().onSizeChanged { navState.mayIncreaseMinWidth(toDP(it.width, density)) }) {
            ContextMenu.Popup(ctmState) { contextMenuFn(navState.selected!!) }
            LazyColumn(
                state = lazyListState, modifier = Modifier.widthIn(min = navState.minWidth)
                    .horizontalScroll(state = rememberScrollState())
                    .pointerMoveFilter(onExit = { navState.hovered = null; false })
            ) {
                navState.entries.forEach {
                    item {
                        ItemLayout(navState, ctmState, it, it.depth, iconArgs) {
                            navState.mayIncreaseMinWidth(toDP(it, density))
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun <T : Navigable.Item<T>> ItemLayout(
        navState: NavigatorState<T>, contextMenuState: ContextMenu.State, item: ItemState<T>, depth: Int,
        iconArgs: (ItemState<T>) -> IconArgs, onSizeChanged: (Int) -> Unit
    ) {
        item.focusReq = remember { FocusRequester() }
        val bgColor = when {
            navState.selected == item -> Theme.colors.primary
            navState.hovered == item -> Theme.colors.indicationBase.copy(INDICATION_HOVER_ALPHA)
            else -> Color.Transparent
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color = bgColor)
                .widthIn(min = navState.minWidth).height(ITEM_HEIGHT)
                .onSizeChanged { onSizeChanged(it.width) }
                .focusRequester(item.focusReq!!).focusable()
                .onKeyEvent { onKeyEvent(it, navState, item) }
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .pointerInput(item) { onPointerInput(contextMenuState, navState, item.focusReq!!, item) }
                .pointerMoveFilter(onEnter = { navState.hovered = item; false })
        ) {
            if (depth > 0) Spacer(modifier = Modifier.width(ICON_WIDTH * depth))
            ItemButton(item)
            ItemIcon(item, iconArgs)
            Spacer(Modifier.width(TEXT_SPACING))
            ItemText(item)
            Spacer(modifier = Modifier.width(AREA_PADDING))
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemButton(item: ItemState<T>) {
        if (item.isExpandable) Form.RawClickableIcon(
            icon = if (item.asExpandable().isExpanded) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_RIGHT,
            onClick = { item.asExpandable().toggle() },
            modifier = Modifier.size(ITEM_HEIGHT)
        ) else Spacer(Modifier.size(ITEM_HEIGHT))
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemIcon(item: ItemState<T>, iconArgs: (ItemState<T>) -> IconArgs) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
            Icon.Render(icon = iconArgs(item).code, color = iconArgs(item).color())
        }
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemText(item: ItemState<T>) {
        Row(modifier = Modifier.height(ICON_WIDTH)) {
            Form.Text(value = item.name)
            item.info?.let {
                Spacer(Modifier.width(TEXT_SPACING))
                Form.Text(value = "( $it )", alpha = 0.4f)
            }
        }
    }

    private suspend fun <T : Navigable.Item<T>> PointerInputScope.onPointerInput(
        contextMenuState: ContextMenu.State, navigatorState: NavigatorState<T>,
        focusReq: FocusRequester, item: ItemState<T>
    ) {
        contextMenuState.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryClick = { navigatorState.select(item); focusReq.requestFocus() },
            onDoublePrimaryClick = { navigatorState.open(item) },
            onSecondaryClick = { navigatorState.select(item) }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun <T : Navigable.Item<T>> onKeyEvent(
        event: KeyEvent, state: NavigatorState<T>, item: ItemState<T>
    ): Boolean {
        return when (event.awtEvent.id) {
            java.awt.event.KeyEvent.KEY_RELEASED -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    if (state.selected == item) state.open(item)
                    else state.select(item)
                    true
                }
                Key.DirectionLeft -> {
                    if (item.isExpandable && item.asExpandable().isExpanded) item.asExpandable().collapse()
                    else state.selectParent(item)
                    true
                }
                Key.DirectionRight -> {
                    if (item.isExpandable && !item.asExpandable().isExpanded) item.asExpandable().expand()
                    else state.selectNext(item)
                    true
                }
                Key.DirectionUp -> {
                    state.selectPrevious(item)
                    true
                }
                Key.DirectionDown -> {
                    state.selectNext(item)
                    true
                }
                else -> false
            }
        }
    }
}