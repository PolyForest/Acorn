/*
 * Copyright 2020 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component.layout

import com.acornui.behavior.Selection
import com.acornui.behavior.SelectionBase
import com.acornui.behavior.retainAll
import com.acornui.behavior.toggleSelected
import com.acornui.collection.ObservableList
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.*
import com.acornui.component.layout.algorithm.virtual.ItemRendererContext
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutAlgorithm
import com.acornui.component.layout.algorithm.virtual.VirtualLayoutDirection
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context

import com.acornui.focus.Focusable
import com.acornui.input.Ascii
import com.acornui.input.KeyState
import com.acornui.input.interaction.MouseEventRo
import com.acornui.input.interaction.click
import com.acornui.input.mouseMove
import com.acornui.input.wheel
import com.acornui.math.*
import com.acornui.recycle.IndexedPool
import com.acornui.recycle.Recycler
import com.acornui.recycle.disposeAndClear

// FIXME: #161 largest renderer?

class DataScroller<E : Any, out S : Style, out T : LayoutData>(
		owner: Context,
		layoutAlgorithm: VirtualLayoutAlgorithm<S, T>,
		val layoutStyle: S
) : ContainerImpl(owner), Focusable {

	val style = bind(DataScrollerStyle())

	private val bottomContents = addChild(virtualList<E, S, T>(layoutAlgorithm, layoutStyle) {
		alpha = 0.0
		interactivityMode = InteractivityMode.NONE
	})

	//---------------------------------------------------
	// Scrolling
	//---------------------------------------------------

	private val isVertical = layoutAlgorithm.direction == VirtualLayoutDirection.VERTICAL

	val scrollModel: ClampedScrollModel
		get() = scrollbar.scrollModel

	/**
	 * The maximum value this data scroller can scroll to.
	 */
	val scrollMax: Double
		get() {
			validate(ValidationFlags.LAYOUT)
			return scrollbar.scrollModel.max
		}

	private val scrollbarClipper = addChild(scrollRect())
	private val scrollbar = scrollbarClipper.addElement(if (isVertical) vScrollbar() else hScrollbar())

	/**
	 * The scroll area is just used for clipping, not scrolling.
	 */
	private val clipper = addChild(scrollRect())

	private val rowBackgrounds = clipper.addElement(container())
	private val rowBackgroundsCache = IndexedPool(Recycler(
			create = { rowBackgrounds.addElement(style.rowBackground(rowBackgrounds)) },
			configure = { it.visible = true },
			unconfigure = { it.visible = false }
	))

	private val contents = clipper.addElement(virtualList<E, S, T>(layoutAlgorithm, layoutStyle) {
		interactivityMode = InteractivityMode.CHILDREN
	})

	private val rowMap = HashMap<E, RowBackground>()

	private val _selection = own(DataScrollerSelection(contents, bottomContents, rowMap))
	val selection: Selection<E> = _selection

	private val _highlighted = own(DataScrollerHighlight(rowMap))

	val highlighted: Selection<E>
		get() = _highlighted


	/**
	 * Determines the behavior of whether or not the scroll bar is displayed.
	 */
	var scrollPolicy by validationProp(ScrollPolicy.AUTO, ValidationFlags.LAYOUT)

	private val tossScroller = clipper.enableTossScrolling()

	//---------------------------------------------------
	// Properties
	//---------------------------------------------------

	/**
	 * The maximum number of items to render before scrolling.
	 * Note that invisible renderers are counted.
	 * If there are explicit dimensions, those are still honored for virtualization.
	 */
	var maxItems: Int
		get() = bottomContents.maxItems
		set(value) {
			contents.maxItems = value + 1
			bottomContents.maxItems = value
		}

	//---------------------------------------------------
	// Item Renderer Pooling
	//---------------------------------------------------

	var selectable: Boolean = true

	/**
	 * If true, multiple items may be selected.
	 *
	 * In multiple selection mode, clicking item renderers toggles their selection.
	 * In single selection mode, clicking an item renderer sets selection to that item.
	 */
	var selectMultiple: Boolean = false

	var highlightable: Boolean = true

	private var background: UiComponent? = null

	private val _mousePosition = vec2()
	private val keyState by KeyState

	init {
		isFocusContainer = true
		focusEnabled = true
		addClass(DataScroller)
		cursor(StandardCursor.POINTER)

		maxItems = 15
		scrollModel.changed.add {
			contents.indexPosition = scrollModel.value
		}
		watch(style) {
			rowBackgroundsCache.disposeAndClear()

			background?.dispose()
			background = addOptionalChild(0, it.background(this))

			scrollbarClipper.style.borderRadii = it.borderRadii
			clipper.style.borderRadii = Corners(
					it.borderRadii.topLeft,
					if (isVertical) Vector2.ZERO else it.borderRadii.topRight,
					Vector2.ZERO,
					if (!isVertical) Vector2.ZERO else it.borderRadii.bottomLeft
			)
		}

		wheel().add {
			val d = if (isVertical) it.deltaY else it.deltaX
			if (!it.handled && scrollModel.max > 0.0 && d != 0.0 && !keyState.keyIsDown(Ascii.CONTROL)) {
				it.handled = true
				tossScroller.stop()
				scrollModel.value += d / scrollbar.modelToPoints
			}
		}

		click().add {
			if (selectable && !it.handled) {
				val e = getElementUnderPosition(mousePosition(_mousePosition))
				if (e != null) {
					it.handled = true
					if (selectMultiple) {
						_selection.toggleSelected(e)
					} else {
						_selection.setSelectedItems(setOf(e), isUserInteraction = true)
					}
				}
			}
		}
		own(DataScrollerTossScrollBinding())
	}

	private inner class DataScrollerTossScrollBinding : TossScrollModelBinding(tossScroller,
			hScrollModel = if (isVertical) ScrollModelImpl() else scrollbar.scrollModel,
			vScrollModel = if (!isVertical) ScrollModelImpl() else scrollbar.scrollModel) {

		override fun localToModel(diffPoints: Vector2) {
			if (contents.activeRenderers.isEmpty()) return
			val aRIndex = contents.activeRenderers.sortedInsertionIndex(contents.visiblePosition) { index, it -> index.compareTo(it.index) } - 1
			val renderer = contents.activeRenderers[aRIndex]
			diffPoints.scl(1.0 / if (isVertical) renderer.height else renderer.width)
		}
	}

	private fun stageMouseMoveHandler(e: MouseEventRo) {
		if (highlightable) updateHighlight()
	}

	/**
	 * Sets the renderer factory for this list. The renderer factory is responsible for creating renderers to be used
	 * in this scroller.
	 */
	fun rendererFactory(value: ItemRendererContext<T>.() -> ListItemRenderer<E>) {
		contents.rendererFactory(value)
		bottomContents.rendererFactory(value)
	}

	/**
	 * Sets the nullRenderer factory for this list. The nullRenderer factory is responsible for creating nullRenderers
	 * to be used in this list.
	 */
	fun nullRendererFactory(value: ItemRendererContext<T>.() -> ListRenderer) {
		contents.nullRendererFactory(value)
		bottomContents.nullRendererFactory(value)
	}

	/**
	 * The data list, as set via [data].
	 */
	val data: List<E?>
		get() = contents.data

	/**
	 * Sets the data source to the given observable list, and watches for changes.
	 */
	fun data(value: ObservableList<E?>?) {
		contents.data(value)
		bottomContents.data(value)
		_selection.data(value)
		_highlighted.data(value)
	}

	/**
	 * Sets the data source to the given non-observable list.
	 */
	fun data(value: List<E?>?) {
		contents.data(value)
		bottomContents.data(value)
		_selection.data(value)
		_highlighted.data(value)
	}

	fun emptyListRenderer(value: ItemRendererContext<T>.() -> UiComponent) {
		contents.emptyListRenderer(value)
		bottomContents.emptyListRenderer(value)
	}

	override fun onActivated() {
		super.onActivated()
		stage.mouseMove().add(::stageMouseMoveHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		stage.mouseMove().remove(::stageMouseMoveHandler)
	}

	private fun updateHighlight() {
		val e = getElementUnderPosition(mousePosition(_mousePosition))
		_highlighted.setSelectedItems(if (e == null) emptySet() else setOf(e), isUserInteraction = true)
	}

	private fun getElementUnderPosition(p: Vector2Ro): E? {
		for (i in 0..rowBackgroundsCache.lastIndex) {
			val bg = rowBackgroundsCache[i]
			if (p.x >= bg.x && p.y >= bg.y && p.x < bg.right && p.y < bg.bottom) {
				return data.getOrNull(bg.rowIndex)
			}
		}
		return null
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		// The typical ScrollArea implementations optimize for the case of not needing scroll bars, but the
		// DataScroller optimizes for the case of needing one. That is, size first with the scroll bar, and if it's not
		// needed, remove it.

		val pad = style.padding
		val w: Double? = pad.reduceWidth(explicitWidth)
		val h: Double? = pad.reduceHeight(explicitHeight)

		if (isVertical) {
			if (scrollPolicy != ScrollPolicy.OFF) {
				// First size as if the scroll bars are needed.
				val vScrollbarW = minOf(w ?: 0.0, scrollbar.minWidth)
				val scrollAreaW = if (w == null) null else w - vScrollbarW

				if (h == null) {
					bottomContents.indexPosition = maxOf(0.0, (data.size - maxItems).toDouble())
				} else {
					bottomContents.bottomIndexPosition = data.lastIndex.toDouble()
				}
				bottomContents.size(scrollAreaW, h)

				if (scrollPolicy == ScrollPolicy.ON || bottomContents.visiblePosition > 0.0) {
					// Keep the scroll bar.
					contents.size(bottomContents.width, h ?: bottomContents.height)
					scrollbar.visible = true
					scrollbar.size(vScrollbarW, bottomContents.height)
					val sBX = if (explicitWidth == null) pad.left + bottomContents.width else explicitWidth - pad.right - vScrollbarW
					scrollbar.position(sBX, pad.top)
				} else {
					// Auto scroll policy and we don't need a scroll bar.
					scrollbar.visible = false
				}
			} else {
				scrollbar.visible = false
			}
			if (!scrollbar.visible) {
				contents.size(w, h)
				bottomContents.size(w, h)
			}
			scrollbar.modelToPoints = bottomContents.height / maxOf(0.0001, bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
		} else {
			if (scrollPolicy != ScrollPolicy.OFF) {
				// First size as if the scroll bars are needed.
				val hScrollbarH = minOf(h ?: 0.0, scrollbar.minHeight)
				val scrollAreaH = if (h == null) null else h - hScrollbarH
				if (w == null) {
					bottomContents.indexPosition = maxOf(0.0, (data.size - maxItems).toDouble())
				} else {
					bottomContents.bottomIndexPosition = data.lastIndex.toDouble()
				}
				bottomContents.size(w, scrollAreaH)

				if (scrollPolicy == ScrollPolicy.ON || bottomContents.visiblePosition > 0.0) {
					// Keep the scroll bar.
					contents.size(w ?: bottomContents.width, bottomContents.height)
					scrollbar.visible = true
					scrollbar.size(bottomContents.width, hScrollbarH)
					val sBY = if (explicitHeight == null) pad.top + bottomContents.height else explicitHeight - pad.bottom - hScrollbarH
					scrollbar.position(pad.left, sBY)
				} else {
					// Auto scroll policy and we don't need a scroll bar.
					scrollbar.visible = false
				}
			} else {
				scrollbar.visible = false
			}
			if (!scrollbar.visible) {
				contents.size(w, h)
				bottomContents.size(w, h)
			}
			scrollbar.modelToPoints = bottomContents.width / maxOf(0.0001, bottomContents.visibleBottomPosition - bottomContents.visiblePosition)
		}

		val scrollbarH = if (!isVertical && scrollbar.visible) scrollbar.height else 0.0
		val scrollbarW = if (isVertical && scrollbar.visible) scrollbar.width else 0.0
		out.set(explicitWidth ?: pad.expandWidth(bottomContents.width + scrollbarW), explicitHeight
				?: pad.expandHeight(bottomContents.height + scrollbarH))
		clipper.size(pad.reduceWidth(out.width - scrollbarW), pad.reduceHeight(out.height - scrollbarH))

		scrollbar.scrollModel.max = bottomContents.visiblePosition

		rowMap.clear()

		val itemRenderers = contents.activeItemRenderers
		for (i in 0..itemRenderers.lastIndex) {
			val itemRenderer = itemRenderers[i]
			val rowBackground = updateRowBackgroundForRenderer(itemRenderer)
			val e = itemRenderer.data ?: continue
			rowMap[e] = rowBackground
			rowBackground.toggled = _selection.getItemIsSelected(e)
			rowBackground.highlighted = _highlighted.getItemIsSelected(e)
		}
		val nullItemRenderers = contents.activeNullRenderers
		for (i in 0..nullItemRenderers.lastIndex) {
			val rowBackground = updateRowBackgroundForRenderer(nullItemRenderers[i])
			rowBackground.toggled = false
			rowBackground.highlighted = false
		}
		rowBackgroundsCache.flip()
		clipper.position(pad.left, pad.top)

		scrollbarClipper.size(out)
		background?.size(out)
	}

	private fun updateRowBackgroundForRenderer(renderer: ListRendererRo): RowBackground {
		val rowBackground = rowBackgroundsCache.obtain(renderer.index)
		rowBackground.rowIndex = renderer.index
		if (isVertical) {
			rowBackground.size(bottomContents.width, renderer.height)
			rowBackground.position(0.0, renderer.y)
		} else {
			rowBackground.size(renderer.width, bottomContents.height)
			rowBackground.position(renderer.x, 0.0)
		}
		return rowBackground
	}

	companion object : StyleTag
}

class DataScrollerStyle : ObservableBase() {

	override val type: StyleType<DataScrollerStyle> = DataScrollerStyle

	/**
	 * The background of the data scroller.
	 */
	var background by prop(noSkinOptional)

	/**
	 * The padding between the background and elements.
	 */
	var padding by prop(Pad(0.0))

	/**
	 * Used for clipping, this should match that of the background border radius.
	 */
	var borderRadii by prop(Corners(0.0))

	/**
	 * The background for each row.
	 */
	var rowBackground by prop<Context.() -> RowBackground> { rowBackground() }

	companion object : StyleType<DataScrollerStyle>
}

private class DataScrollerSelection<E : Any>(
		private val listA: VirtualList<E, *, *>,
		private val listB: VirtualList<E, *, *>,
		private val rowMap: Map<E, RowBackground>
) : SelectionBase<E>() {

	private var data = emptyList<E?>()

	fun data(value: List<E?>?) {
		val newData = value ?: emptyList()
		retainAll(newData.filterNotNull(), isUserInteraction = false)
		data = newData
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onSelectionChanged(oldSelection: Set<E>, newSelection: Set<E>) {
		listA.selection.setSelectedItems(newSelection, isUserInteraction = true)
		listB.selection.setSelectedItems(newSelection, isUserInteraction = true)
		for (e in oldSelection - newSelection) {
			rowMap[e]?.toggled = false
		}
		for (e in newSelection - oldSelection) {
			rowMap[e]?.toggled = true
		}
	}
}

private class DataScrollerHighlight<E : Any>(private val rowMap: Map<E, RowBackground>) : SelectionBase<E>() {

	private var data = emptyList<E?>()

	fun data(value: List<E?>?) {
		val newData = value ?: emptyList()
		retainAll(newData.filterNotNull(), isUserInteraction = false)
		data = newData
	}

	override fun walkSelectableItems(callback: (E) -> Unit) {
		for (i in 0..data.lastIndex) {
			val item = data[i] ?: continue
			callback(item)
		}
	}

	override fun onSelectionChanged(oldSelection: Set<E>, newSelection: Set<E>) {
		for (e in oldSelection - newSelection) {
			rowMap[e]?.highlighted = false
		}
		for (e in newSelection - oldSelection) {
			rowMap[e]?.highlighted = true
		}
	}
}