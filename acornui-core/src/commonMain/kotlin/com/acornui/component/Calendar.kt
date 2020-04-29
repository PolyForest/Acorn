/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.component

import com.acornui.behavior.Selection
import com.acornui.behavior.SelectionBase
import com.acornui.collection.Filter
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.*
import com.acornui.component.layout.spacer
import com.acornui.component.style.*
import com.acornui.component.text.TextField
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.focus.FocusOptions
import com.acornui.focus.focus
import com.acornui.graphic.Color
import com.acornui.input.Ascii
import com.acornui.input.interaction.ClickInteractionRo
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.MouseOrTouchState
import com.acornui.input.interaction.click
import com.acornui.input.keyDown
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.observe.bind
import com.acornui.properties.afterChange
import com.acornui.system.userInfo
import com.acornui.text.DateTimeFormatStyle
import com.acornui.text.DateTimeFormatType
import com.acornui.text.dateFormatter
import com.acornui.text.dateTimeFormatter
import com.acornui.time.Date
import com.acornui.time.DateRo
import com.acornui.zeroPadding
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class Calendar(
		owner: Context
) : ContainerImpl(owner) {

	/**
	 * If true, clicking on a calendar cell will set the selection to that cell.
	 * This does not affect setting the selection via code.
	 */
	var selectable = true

	private var _headerFactory: Context.() -> Labelable = { text { charStyle.selectable = false } }
	private var _rendererFactory: Context.() -> CalendarItemRenderer = { calendarItemRenderer() }
	private val grid = grid()
	private val monthYearText: TextField

	private val yearFormatter = dateTimeFormatter(DateTimeFormatType.YEAR)
	private val monthFormatter = dateTimeFormatter(DateTimeFormatType.MONTH)

	/**
	 * If set, only the dates that pass the filter will be enabled.
	 */
	var dateEnabledFilter: Filter<DateRo>? by validationProp(null, ValidationFlags.PROPERTIES)

	/**
	 * If the [dateEnabledFilter] needs to be re-evaluated, this method will ensure that the filter is rechecked.
	 */
	fun invalidateDateEnabled() {
		invalidateProperties()
	}

	val selection: Selection<DateRo> = own(object : SelectionBase<DateRo>() {
		override fun walkSelectableItems(callback: (item: DateRo) -> Unit) {
			for (i in 0..cells.lastIndex) {
				val date = cells[i].data ?: continue
				callback(date)
			}
		}

		override fun onSelectionChanged(oldSelection: Set<DateRo>, newSelection: Set<DateRo>) {
			for (i in 0..cells.lastIndex) {
				val cell = cells[i]
				val d = cell.data
				cell.toggled = if (d == null) false else getItemIsSelected(d)
			}
		}
	})

	val highlighted: Selection<DateRo> = own(object : SelectionBase<DateRo>() {
		override fun walkSelectableItems(callback: (item: DateRo) -> Unit) {
			for (i in 0..cells.lastIndex) {
				val date = cells[i].data ?: continue
				callback(date)
			}
		}

		override fun onSelectionChanged(oldSelection: Set<DateRo>, newSelection: Set<DateRo>) {
		}
	})

	fun headerFactory(value: Context.() -> Labelable) {
		val headers = _headers
		if (headers != null) {
			// Dispose old header cells when switching renderer factories.
			for (i in 0..headers.lastIndex) {
				headers[i].dispose()
			}
			_headers = null
		}
		_headerFactory = value
		invalidateLayout()
	}

	private var _headers: Array<Labelable>? = null

	private val headers: Array<Labelable>
		get() {
			if (_headers == null) {
				grid.apply {
					_headers = Array(7) {
						addElement(it, _headerFactory())
					}
				}
			}
			return _headers!!
		}

	fun rendererFactory(value: Context.() -> CalendarItemRenderer) {
		val cells = _cells
		if (cells != null) {
			// Dispose old cells when switching renderer factories.
			for (i in 0..cells.lastIndex) {
				cells[i].dispose()
			}
			_cells = null // Cells will be recreated using the new renderer factory on next access.
		}
		_rendererFactory = value
		invalidateProperties()
	}

	private var _cells: Array<CalendarItemRenderer>? = null
	protected val cells: Array<CalendarItemRenderer>
		get() {
			if (_cells == null) {
				grid.apply {
					_cells = Array(42) {
						+_rendererFactory().apply {
							index = it
							setActiveMonth(month, fullYear)
							click().add(this@Calendar::cellClickedHandler)
						}
					}
				}
			}
			return _cells!!
		}

	private fun cellClickedHandler(e: ClickInteractionRo) {
		if (selectable && !e.handled) {
			@Suppress("UNCHECKED_CAST")
			val cell = e.currentTarget as CalendarItemRenderer
			e.handled = true
			selection.setSelectedItems(setOf(cell.data!!), isUserInteraction = true)
		}
	}

	protected fun getCellByDate(cellDate: DateRo): CalendarItemRenderer? {
		val dayOffset = date.dayOfWeek - dayOfWeek
		val i = dayOffset + cellDate.dayOfMonth - 1
		return cells[i]
	}

	private val monthDecContainer: ElementContainer<UiComponent>
	private val monthIncContainer: ElementContainer<UiComponent>
	private var monthDecButton: UiComponent? = null
	private var monthIncButton: UiComponent? = null

	private val panel = addChild(panel {
		+vGroup {
			+hGroup {
				style.verticalAlign = VAlign.MIDDLE

				monthDecContainer = +stack {
					click().add {
						month--
					}
				}

				+spacer() layout { widthPercent = 1f }

				monthYearText = +text {
					selectable = false
					text = ""
				}
				+spacer() layout { widthPercent = 1f }

				monthIncContainer = +stack {
					click().add {
						month++
					}
				}
			} layout { widthPercent = 1f }

			+grid layout {
				fill()
				priority = 1f
			}
		} layout { fill() }
	})

	val panelStyle: PanelStyle
		get() = panel.style

	val style = bind(CalendarStyle())

	val layoutStyle: GridLayoutStyle
		get() = grid.style

	private val date = Date().apply {
		hour = 0
		minute = 0
		second = 0
		milli = 0
		dayOfMonth = 1
	}

	/**
	 * The month of the year. This is 1-based.  January = 1, December = 12
	 */
	var month: Int
		get() = date.month
		set(value) {
			date.month = value
			invalidateProperties()
		}

	/**
	 * The 4 digit year.
	 */
	var fullYear: Int
		get() = date.fullYear
		set(value) {
			date.fullYear = value
			invalidateProperties()
		}

	/**
	 * The starting day of the week. (0 is Sunday, 6 is Saturday)
	 */
	var dayOfWeek: Int by validationProp(0, ValidationFlags.PROPERTIES)

	private val weekDayFormatter = dateFormatter {
		type = DateTimeFormatType.WEEKDAY
		dateStyle = DateTimeFormatStyle.SHORT
	}

	init {
		isFocusContainer = true
		focusEnabled = true
		styleTags.add(Companion)
		validation.addNode(ValidationFlags.PROPERTIES, ValidationFlags.STYLES, ValidationFlags.LAYOUT, ::updateProperties)

		bind(userInfo.currentLocale) {
			// If the locale changes, the weekday headers and month names change.
			invalidateProperties()
		}

		watch(style) {
			monthFormatter.dateStyle = it.monthFormatStyle
			yearFormatter.dateStyle = it.yearFormatStyle

			monthDecButton?.dispose()
			monthDecButton = monthDecContainer.addElement(it.monthDecButton(this))

			monthIncButton?.dispose()
			monthIncButton = monthIncContainer.addElement(it.monthIncButton(this))
		}

		keyDown().add(::keyDownHandler)
	}

	private fun updateProperties() {
		val dayOffset = date.dayOfWeek - dayOfWeek

		for (i in 0..6) {
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			headers[i].label = weekDayFormatter.format(iDate)
		}
		for (i in 0..41) {
			val cell = cells[i]
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			cell.data = iDate
			cell.toggled = selection.getItemIsSelected(iDate)
			cell.disabled = dateEnabledFilter?.invoke(iDate) == false
			cell.setActiveMonth(month, fullYear)
		}
		monthYearText.text = monthFormatter.format(date) + " " + yearFormatter.format(date)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		var maxHeaderW = 0f
		for (i in 0..6) {
			maxHeaderW = maxOf(maxHeaderW, headers[i].width)
		}
		val columns = ArrayList<GridColumn>()
		for (i in 0..6) {
			columns.add(GridColumn(hAlign = style.columnHAlign, widthPercent = 1f, minWidth = maxHeaderW))
		}
		grid.apply {
			style.columns = columns
			style.rowHeight = if (explicitHeight == null) null else (explicitHeight - 6f * style.verticalGap) / 7f
			cells.forEach {
				if (it.layoutData == null) {
					it.layoutData = grid.createLayoutData()
				}
				(it.layoutData as GridLayoutData).let { layoutData ->
					layoutData.widthPercent = 1f
					layoutData.heightPercent = if (explicitHeight == null) null else 1f
				}
			}
		}

		panel.size(explicitWidth, explicitHeight)
		out.set(panel.bounds)
	}

	private fun keyDownHandler(e: KeyInteractionRo) {
		when (e.keyCode) {
			Ascii.UP -> moveSelectedCell(0, -1)
			Ascii.RIGHT -> moveSelectedCell(1, 0)
			Ascii.DOWN -> moveSelectedCell(0, 1)
			Ascii.LEFT -> moveSelectedCell(-1, 0)
		}
	}

	private fun moveSelectedCell(xD: Int, yD: Int) {
		val focusedIndex = cells.indexOf(focusManager.focused)
		val currentRow: Int
		val currentCol: Int
		if (focusedIndex == -1) {
			currentCol = if (xD > 0) -1 else 0
			currentRow = if (yD > 0) -1 else 0
		} else {
			currentRow = focusedIndex / 7
			currentCol = focusedIndex % 7
		}

		var row = currentRow
		var col = currentCol
		for (i in 0..7) {
			row += yD
			col += xD
			if (col < 0) {
				col = 6
				row -= 1
			} else if (col >= 7) {
				col = 0
				row += 1
			}
			if (row < 0) {
				row = 6
			} else if (row > 6) {
				row = 0
			}
			val cell = cells.getOrNull(row * 7 + col)
			if (cell != null && cell.focusEnabled && cell.isRendered && cell.interactivityEnabled) {
				cell.focus(FocusOptions.highlight)
				break
			}
		}
	}

	companion object : StyleTag
}

class CalendarStyle : StyleBase() {

	override val type: StyleType<*> = Companion

	var monthDecButton by prop(noSkin)
	var monthIncButton by prop(noSkin)

	var columnHAlign by prop(HAlign.CENTER)

	var monthFormatStyle by prop(DateTimeFormatStyle.LONG)
	var yearFormatStyle by prop(DateTimeFormatStyle.FULL)

	companion object : StyleType<CalendarStyle>
}

inline fun Context.calendar(init: ComponentInit<Calendar> = {}): Calendar  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = Calendar(this)
	c.init()
	return c
}

interface CalendarItemRenderer : ListItemRenderer<DateRo> {

	/**
	 * @param month The active month of the year. This is 1-based.  January = 1, December = 12
	 * @param fullYear The active 4 digit year.
	 */
	fun setActiveMonth(month: Int, fullYear: Int)

	/**
	 * True if the calendar cell should be disabled.
	 */
	var disabled: Boolean

}

open class CalendarItemRendererImpl(owner: Context) : ContainerImpl(owner), CalendarItemRenderer {

	val style = bind(CalendarItemRendererStyle())

	private val mouseOrTouchState = own(MouseOrTouchState(this)).apply {
		isOverChanged.add { refreshColor() }
		isDownChanged.add { refreshColor() }
	}

	private val background = addChild(rect {
		style.backgroundColor = Color.WHITE
		style.borderThicknesses = Pad(1f)
		style.borderColors = BorderColors(Color(0f, 0f, 0f, 0.3f))
	})

	private val textField = addChild(text {
		selectable = false
	})

	override var toggled: Boolean by afterChange(false) {
		refreshColor()
	}

	override var disabled: Boolean by afterChange(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
		refreshColor()
	}

	private var isInActiveMonth: Boolean by afterChange(false) {
		if (it) {
			styleTags.remove(INACTIVE)
			styleTags.add(ACTIVE)
		} else {
			styleTags.remove(ACTIVE)
			styleTags.add(INACTIVE)
		}
	}

	private fun refreshIsInActiveMonth() {
		val date = _data
		val v = date != null && date.month == _month && date.fullYear == _fullYear
		isInActiveMonth = v
	}

	private var _data: DateRo? = null
	override var data: DateRo?
		get() = _data
		set(value) {
			if (_data == value) return
			_data = value
			refreshLabel()
			refreshIsInActiveMonth()
		}

	override var index: Int = 0

	init {
		styleTags.add(Companion)
		styleTags.add(INACTIVE)
		focusEnabled = true
		cursor(StandardCursor.HAND)
		watch(style) {
			refreshLabel()
			refreshColor()
		}
	}

	private var _month: Int = -1
	private var _fullYear: Int = -1

	override fun setActiveMonth(month: Int, fullYear: Int) {
		_month = month
		_fullYear = fullYear
		refreshIsInActiveMonth()
	}

	private fun refreshLabel() {
		textField.label = _data?.dayOfMonth?.zeroPadding(if (style.zeroPadDay) 1 else 0) ?: ""
	}

	private fun refreshColor() {
		background.colorTint = when (calculateState()) {
			ButtonState.UP -> style.upColor
			ButtonState.OVER -> style.overColor
			ButtonState.DOWN -> style.downColor
			ButtonState.TOGGLED_UP,
			ButtonState.INDETERMINATE_UP -> style.toggledUpColor
			ButtonState.TOGGLED_OVER,
			ButtonState.INDETERMINATE_OVER -> style.toggledOverColor
			ButtonState.TOGGLED_DOWN,
			ButtonState.INDETERMINATE_DOWN -> style.toggledDownColor
			ButtonState.DISABLED -> style.disabledColor
		}
	}

	private fun calculateState(): ButtonState {
		return if (disabled) {
			ButtonState.DISABLED
		} else {
			if (toggled) {
				when {
					mouseOrTouchState.isDown -> ButtonState.TOGGLED_DOWN
					mouseOrTouchState.isOver -> ButtonState.TOGGLED_OVER
					else -> ButtonState.TOGGLED_UP
				}
			} else {
				when {
					mouseOrTouchState.isDown -> ButtonState.DOWN
					mouseOrTouchState.isOver -> ButtonState.OVER
					else -> ButtonState.UP
				}
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val pad = style.padding
		textField.size(pad.reduceWidth(explicitWidth), pad.reduceHeight(explicitHeight))
		textField.position(pad.left, pad.top)
		background.size(pad.expandWidth(textField.width), pad.expandHeight(textField.height))
		out.set(background.width, background.height, textField.baselineY)
	}

	companion object : StyleTag {

		/**
		 * The tag added when the date is within the active month.
		 */
		val ACTIVE = styleTag()

		/**
		 * The tag added when the date is not within the active month.
		 */
		val INACTIVE = styleTag()
	}
}

open class CalendarItemRendererStyle : StyleBase() {

	override val type: StyleType<CalendarItemRendererStyle> = Companion

	/**
	 * If true, single digit days will be rendered with zeros.  E.g.  "1" becomes "01"
	 */
	var zeroPadDay by prop(false)

	var padding by prop(Pad(2f))

	var disabledColor by prop(Color(0.5f, 0.5f, 0.5f, 0.6f))
	var upColor by prop(Color(1f, 1f, 1f, 0.6f))
	var overColor by prop(Color(1f, 1f, 0.5f, 0.6f))
	var downColor by prop(Color(0.6f, 0.6f, 0.5f, 0.6f))
	var toggledUpColor by prop(Color(1f, 1f, 0f, 0.4f))
	var toggledOverColor by prop(Color(1f, 1f, 0f, 0.6f))
	var toggledDownColor by prop(Color(1f, 1f, 0f, 0.3f))

	companion object : StyleType<CalendarItemRendererStyle>
}

inline fun Context.calendarItemRenderer(init: ComponentInit<CalendarItemRendererImpl> = {}): CalendarItemRendererImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = CalendarItemRendererImpl(this)
	c.init()
	return c
}
