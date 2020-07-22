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

import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.hGroup
import com.acornui.component.style.*
import com.acornui.component.text.*
import com.acornui.cursor.StandardCursor
import com.acornui.cursor.cursor
import com.acornui.di.Context

import com.acornui.focus.*
import com.acornui.gl.core.TextureMagFilter
import com.acornui.graphic.*
import com.acornui.input.interaction.click
import com.acornui.input.interaction.dragAttachment
import com.acornui.input.interaction.isEnterOrReturn
import com.acornui.input.keyDown
import com.acornui.math.*
import com.acornui.popup.lift
import com.acornui.properties.afterChange
import com.acornui.signal.Signal1
import com.acornui.toInt
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ColorPicker(owner: Context) : ContainerImpl(owner), InputComponent<ColorRo> {

	val style = bind(ColorPickerStyle())

	private val _changed = own(Signal1<ColorPicker>())
	override val changed = _changed.asRo()

	private var background: UiComponent? = null
	private var colorSwatch: UiComponent? = null
	private val colorPalette = colorPalette {
		changed.add {
			_changed.dispatch(this@ColorPicker)
		}
	}

	private val colorPaletteLift = lift {
		focus = true
		+colorPalette
		onClosed = this@ColorPicker::close
	}

	override var value: ColorRo
		get() = colorPalette.value
		set(value) {
			val v = value.copy().clamp()
			colorPalette.value = v
			colorSwatch?.colorTint = v
		}

	var hsv: HsvRo
		get() = colorPalette.hsv
		set(value) {
			colorPalette.hsv = value
			colorSwatch?.colorTint = value.toRgb(tmpColor).copy().clamp()
		}

	fun userChange(value: ColorRo) {
		colorPalette.userChange(value)
		colorSwatch?.colorTint = value.copy().clamp()
	}

	fun userChange(value: HsvRo) {
		colorPalette.userChange(value)
		colorSwatch?.colorTint = value.toRgb(tmpColor).copy().clamp()
	}

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker: Boolean
		get() = colorPalette.showValuePicker
		set(value) {
			colorPalette.showValuePicker = value
		}

	/**
	 * If true (default), there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker: Boolean
		get() = colorPalette.showAlphaPicker
		set(value) {
			colorPalette.showAlphaPicker = value
		}


	private val tmpColor = Color()

	init {
		focusEnabled = true
		addClass(ColorPicker)

		click().add {
			isOpen = !isOpen
		}

		colorPalette.changed.add {
			colorSwatch?.colorTint = hsv.toRgb(tmpColor).copy()
		}

		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))
			colorSwatch?.dispose()
			colorSwatch = addChild(it.colorSwatch(this)).apply {
				colorTint = this@ColorPicker.value
				interactivityMode = InteractivityMode.NONE
			}

		}

		blurredAll(this, colorPalette).add {
			close()
		}
	}

	var isOpen: Boolean = false
		set(value) {
			if (field == value) return
			field = value
			if (value) {
				addChild(colorPaletteLift)
			} else {
				removeChild(colorPaletteLift)
			}
		}

	fun open() {
		isOpen = true
	}

	fun close() {
		isOpen = false
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val background = background ?: return
		val colorSwatch = colorSwatch ?: return
		val s = style
		val padding = s.padding
		colorSwatch.size(
				padding.reduceWidth(explicitWidth) ?: s.defaultSwatchWidth,
				padding.reduceHeight(explicitHeight) ?: s.defaultSwatchHeight
		)
		val measuredW = padding.expandWidth(colorSwatch.width)
		val measuredH = padding.expandHeight(colorSwatch.height)
		background.size(measuredW, measuredH)
		colorSwatch.position(0.5 * (background.width - colorSwatch.width), 0.5 * (background.height - colorSwatch.height))
		out.set(background.width, background.height, colorSwatch.bottom)

		colorPaletteLift.position(0.0, measuredH)
	}

	override fun dispose() {
		super.dispose()
		close()
	}

	companion object : StyleTag {
		val COLOR_SWATCH_STYLE = styleTag()
	}
}

class ColorPickerStyle : ObservableBase() {

	override val type = Companion

	var padding by prop<PadRo>(Pad(2.0))
	var background by prop(noSkin)
	var defaultSwatchWidth by prop(14.0)
	var defaultSwatchHeight by prop(14.0)
	var colorSwatch by prop(noSkin)

	companion object : StyleType<ColorPickerStyle>
}

inline fun Context.colorPicker(init: ComponentInit<ColorPicker> = {}): ColorPicker {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = ColorPicker(this)
	c.init()
	return c
}

class ColorPalette(owner: Context) : ContainerImpl(owner), InputComponent<ColorRo> {

	private val _changed = own(Signal1<ColorPalette>())
	override val changed = _changed.asRo()

	private val handleWidth = 7.0

	val style = bind(ColorPaletteStyle())

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker by afterChange(true) {
		valueRect.visible = it
		valueIndicator?.visible = it
		invalidateLayout()
	}

	/**
	 * If true, there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker by afterChange(true) {
		alphaRect.visible = it
		alphaGrid.visible = it
		alphaIndicator?.visible = it
		invalidateLayout()
	}

	private var background: UiComponent? = null
	private var hueSaturationIndicator: UiComponent? = null
	private var valueIndicator: UiComponent? = null
	private var alphaIndicator: UiComponent? = null

	private val hueRect = addChild(rect {
		includeInLayout = false
		style.linearGradient = LinearGradient(GradientDirection.RIGHT,
				Color(1.0, 0.0, 0.0, 1.0),
				Color(1.0, 1.0, 0.0, 1.0),
				Color(0.0, 1.0, 0.0, 1.0),
				Color(0.0, 1.0, 1.0, 1.0),
				Color(0.0, 0.0, 1.0, 1.0),
				Color(1.0, 0.0, 1.0, 1.0),
				Color(1.0, 0.0, 0.0, 1.0)
		)
	})

	private val saturationRect = addChild(rect {
		includeInLayout = false
		style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				Color(1.0, 1.0, 1.0, 0.0),
				Color(1.0, 1.0, 1.0, 1.0)
		)
		cursor(StandardCursor.CROSSHAIR)

		dragAttachment(0.0).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			tmpHSV.set(hsv)
			tmpHSV.h = 360.0 * com.acornui.math.clamp(tmpVec.x / width, 0.0, 1.0)
			tmpHSV.s = 1.0 - com.acornui.math.clamp(tmpVec.y / height, 0.0, 1.0)

			userChange(tmpHSV)
		}
	})

	/**
	 * Sets the value and triggers a changed signal.
	 */
	fun userChange(value: HsvRo) {
		val previous = this.hsv
		if (previous == value) return
		this.hsv = value
		_changed.dispatch(this)
	}

	/**
	 * Sets the color and triggers a changed signal.
	 */
	fun userChange(value: ColorRo) {
		val previous = this.value
		if (previous == value) return
		this.value = value
		_changed.dispatch(this)
	}

	private val valueRect = addChild(rect {
		style.margin = Pad(handleWidth, 0.0, 0.0, 0.0)
		dragAttachment(0.0).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = com.acornui.math.clamp(tmpVec.y / height, 0.0, 1.0)

			tmpHSV.set(hsv)
			tmpHSV.v = 1.0 - p
			userChange(tmpHSV)
		}
	})

	private val alphaGrid = addChild(repeatingTexture("assets/uiskin/AlphaCheckerboard_1x.png") {
		// We don't need the hdpi texture here for the checkerboard if the mag filter is NEAREST
		filterMag = TextureMagFilter.NEAREST
	})

	private val alphaRect = addChild(rect {
		style.margin = Pad(handleWidth, 0.0, 0.0, 0.0)
		dragAttachment(0.0).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = com.acornui.math.clamp(tmpVec.y / height, 0.0, 1.0)

			tmpHSV.set(hsv)
			tmpHSV.a = 1.0 - p
			userChange(tmpHSV)
		}
	})

	private var _value: ColorRo = Color.WHITE
	override var value: ColorRo
		get() = _value
		set(value) {
			if (_value != value) {
				_value = value.copy()
				_hsv = value.toHsv()
				invalidate(COLORS)
			}
		}

	private var _hsv: HsvRo = Color.WHITE.toHsv(Hsv())
	var hsv: HsvRo
		get() = _hsv
		set(value) {
			if (_hsv != value) {
				_hsv = value.copy()
				_value = value.toRgb()
				invalidate(COLORS)
			}
		}

	init {
		focusEnabled = true
		addClass(ColorPalette)
		watch(style) {
			background?.dispose()
			hueSaturationIndicator?.dispose()
			valueIndicator?.dispose()
			alphaIndicator?.dispose()

			background = addChild(0, it.background(this))

			hueSaturationIndicator = addChild(it.hueSaturationIndicator(this))
			hueSaturationIndicator!!.interactivityMode = InteractivityMode.NONE

			valueIndicator = addChild(it.sliderArrow(this))
			valueIndicator!!.interactivityMode = InteractivityMode.NONE
			valueIndicator?.visible = showValuePicker

			alphaIndicator = addChild(it.sliderArrow(this))
			alphaIndicator!!.interactivityMode = InteractivityMode.NONE
			alphaIndicator?.visible = showAlphaPicker
		}

		validation.addNode(COLORS, ValidationFlags.STYLES, ValidationFlags.LAYOUT, ::updateColors)
	}

	private fun updateColors() {
		tmpHSV.set(hsv)
		tmpHSV.v = 1.0
		tmpHSV.a = 1.0
		valueRect.style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				tmpHSV.toRgb(tmpColor).copy(),
				Color(0.0, 0.0, 0.0, 1.0)
		)
		alphaRect.style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				tmpHSV.toRgb(),
				Color(0.0, 0.0, 0.0, 0.0)
		)

	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val s = style
		val padding = s.padding

		val numSliders = showValuePicker.toInt() + showAlphaPicker.toInt()
		val w = explicitWidth
				?: s.defaultPaletteWidth + numSliders * (s.sliderWidth + s.gap) + padding.left + padding.right
		val h = explicitHeight ?: s.defaultPaletteHeight + padding.top + padding.bottom

		hueRect.size(w - padding.right - padding.left - numSliders * (s.sliderWidth + s.gap), h - padding.top - padding.bottom)
		hueRect.position(padding.left, padding.top)
		saturationRect.size(hueRect.width, hueRect.height)
		saturationRect.position(hueRect.x, hueRect.y)

		val sliderHeight = h - padding.top - padding.bottom
		valueRect.size(s.sliderWidth + handleWidth, sliderHeight)
		valueRect.position(hueRect.right + s.gap - handleWidth, padding.top)

		alphaGrid.size(s.sliderWidth, sliderHeight)
		alphaGrid.position(valueRect.right + s.gap, padding.top)
		alphaRect.size(s.sliderWidth + handleWidth, sliderHeight)
		alphaRect.position(valueRect.right + s.gap - handleWidth, padding.top)

		hueSaturationIndicator!!.position(saturationRect.x + hsv.h / 360.0 * saturationRect.width - hueSaturationIndicator!!.width * 0.5, saturationRect.y + (1.0 - hsv.s) * saturationRect.height - hueSaturationIndicator!!.height * 0.5)
		valueIndicator!!.position(valueRect.x + handleWidth - valueIndicator!!.width * 0.5, (1.0 - hsv.v) * sliderHeight + padding.top - valueIndicator!!.height * 0.5)
		alphaIndicator!!.position(alphaRect.x + handleWidth - alphaIndicator!!.width * 0.5, (1.0 - hsv.a) * sliderHeight + padding.top - alphaIndicator!!.height * 0.5)

		val bg = background!!
		bg.size(w, h)
		out.set(bg.width, bg.height)
	}

	companion object : StyleTag {
		private val tmpVec = vec2()
		private val tmpHSV = Hsv()
		private val tmpColor = Color()

		private const val COLORS = 1 shl 16
	}

}

inline fun Context.colorPalette(init: ComponentInit<ColorPalette> = {}): ColorPalette {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ColorPalette(this).apply(init)
}

class ColorPaletteStyle : ObservableBase() {

	override val type = Companion

	var padding by prop(Pad(7.0))
	var sliderWidth by prop(16.0)
	var defaultPaletteWidth by prop(200.0)
	var defaultPaletteHeight by prop(100.0)
	var gap by prop(5.0)
	var background by prop(noSkin)
	var hueSaturationIndicator by prop(noSkin)
	var sliderArrow by prop(noSkin)

	companion object : StyleType<ColorPaletteStyle>
}

/**
 * A Color picker with a text input for a hexdecimal color representation.
 */
class ColorPickerWithText(owner: Context) : ContainerImpl(owner), InputComponent<ColorRo> {

	private val _changed = Signal1<ColorPickerWithText>()
	override val changed = _changed.asRo()

	override var value: ColorRo
		get() = colorPicker.value
		set(value) {
			colorPicker.value = value
			updateText()
		}

	var hsv: HsvRo
		get() = colorPicker.hsv
		set(value) {
			colorPicker.hsv = value
			updateText()
		}

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker: Boolean
		get() = colorPicker.showValuePicker
		set(value) {
			colorPicker.showValuePicker = value
		}

	/**
	 * If true (default), there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker: Boolean
		get() = colorPicker.showAlphaPicker
		set(value) {
			colorPicker.showAlphaPicker = value
		}

	private val textInput: TextInputImpl = textInput {
		restrictPattern = RestrictPatterns.COLOR
		visible = false
		changed.add {
			val c = text.toColorOrNull()
			if (c != null) {
				colorPicker.userChange(c)
				updateText()
				closeTextEditor()
			}
		}
	}

	private val text = text("") {
		focusEnabled = true
		selectable = false
		cursor(StandardCursor.POINTER)
	}

	private val colorPicker: ColorPicker = colorPicker {
		changed.add {
			updateText()
			_changed.dispatch(this@ColorPickerWithText)
		}
	}

	private val hGroup = addChild(hGroup {
		style.verticalAlign = VAlign.BASELINE
		+colorPicker
		+textInput
		+text
	})

	init {
		addClass(Companion)

//		isFocusContainer = true
//
//		colorPicker.focusEnabled = false
//		colorPicker.focusEnabledChildren = false

		keyDown().add {
			if (!it.handled && it.isEnterOrReturn) {
				it.handled = true
				// If enter or return is pressed -
				// If the editor is open and hasn't changed (handled will be false), close the editor
				// If the editor is closed, open it
				textEditorIsOpen = !textEditorIsOpen
			}
		}

		focused().add {
			openTextEditor()
		}

		blurred().add {
			closeTextEditor()
		}
	}

	var textEditorIsOpen: Boolean = false
		set(value) {
			if (field == value) return
			field = value
			textInput.visible = value
			text.visible = !value
			textInput.focus()
		}

	fun openTextEditor() {
		textEditorIsOpen = true
	}

	fun closeTextEditor() {
		textEditorIsOpen = false
	}

	var isOpen: Boolean
		get() = colorPicker.isOpen
		set(value) {
			colorPicker.isOpen = value
		}

	fun open() = colorPicker.open()

	fun close() = colorPicker.close()

	private fun updateText() {
		val str = "#" + value.toRgbaString()
		textInput.text = str
		text.text = str
	}

	private val pad = Pad()

	override fun updateStyles() {
		super.updateStyles()
		textInput.validate(ValidationFlags.STYLES)
		val textInputStyle = textInput.textInputStyle
		text.flowStyle.padding = pad.set(left = 0.0, top = textInputStyle.margin.top + textInputStyle.padding.top, right = 0.0, bottom = textInputStyle.margin.bottom + textInputStyle.padding.bottom)
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		hGroup.size(explicitWidth, explicitHeight)
		out.set(hGroup.bounds)
	}

	companion object : StyleTag
}

inline fun Context.colorPickerWithText(init: ComponentInit<ColorPickerWithText> = {}): ColorPickerWithText {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = ColorPickerWithText(this)
	c.init()
	return c
}