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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "UnsafeCastFromDynamic")

package com.acornui.js.gl

import com.acornui.core.WindowConfig
import com.acornui.core.browser.Location
import com.acornui.core.graphic.Window
import com.acornui.function.as1
import com.acornui.gl.core.Gl20
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.js.window.JsLocation
import com.acornui.logging.Log
import com.acornui.signal.*
import org.w3c.dom.BeforeUnloadEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window

/**
 * @author nbilyk
 */
class WebGlWindowImpl(
		private val canvas: HTMLCanvasElement,
		config: WindowConfig,
		private val gl: Gl20) : Window {

	private val cancel = Cancel()
	private val _closeRequested = Signal1<Cancel>()
	override val closeRequested: Signal<(Cancel) -> Unit> = _closeRequested.asRo()

	private val _isActiveChanged: Signal1<Boolean> = Signal1()
	override val isActiveChanged = _isActiveChanged.asRo()

	private val _isVisibleChanged: Signal1<Boolean> = Signal1()
	override val isVisibleChanged = _isVisibleChanged.asRo()

	private val _sizeChanged: Signal3<Float, Float, Boolean> = Signal3()
	override val sizeChanged = _sizeChanged.asRo()

	private val _scaleChanged: Signal2<Float, Float> = Signal2()
	override val scaleChanged = _scaleChanged.asRo()

	private var _width: Float = 0f
	private var _height: Float = 0f
	private var sizeIsDirty: Boolean = true

	// Visibility properties
	private var hiddenProp: String? = null
	private val hiddenPropEventMap = hashMapOf(
			"hidden" to "visibilitychange",
			"mozHidden" to "mozvisibilitychange",
			"webkitHidden" to "webkitvisibilitychange",
			"msHidden" to "msvisibilitychange")

	private var _clearColor = Color.CLEAR.copy()

	override var clearColor: ColorRo
		get() = _clearColor
		set(value) {
			_clearColor.set(value)
			gl.clearColor(value)
			requestRender()
		}

	private val scaleQuery = window.matchMedia("(resolution: ${window.devicePixelRatio}dppx")

	init {
		setSizeInternal(canvas.offsetWidth.toFloat(), canvas.offsetHeight.toFloat(), isUserInteraction = true)

		window.addEventListener("resize", ::resizeHandler.as1)
		canvas.addEventListener("webglcontextrestored", ::webGlContextRestoredHandler.as1)
		window.addEventListener("blur", ::blurHandler.as1)
		window.addEventListener("focus", ::focusHandler.as1)

		// Watch for devicePixelRatio changes.
		scaleQuery.addListener(::scaleChangedHandler.as1)

		canvas.addEventListener("selectstart", { it.preventDefault() })
		if (config.title.isNotEmpty())
			document.title = config.title

		watchForVisibilityChanges()

		clearColor = config.backgroundColor
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
		document.addEventListener("fullscreenchange", ::fullScreenChangedHandler.as1)

		window.addEventListener("beforeunload", {
			event ->
			event as BeforeUnloadEvent
			_closeRequested.dispatch(cancel.reset())
			if (cancel.canceled) {
				event.preventDefault()
				event.returnValue = ""
			}
		})
	}

	private fun scaleChangedHandler() {
		val oldScaleX = scaleX
		val oldScaleY = scaleY
		val newScale = window.devicePixelRatio.toFloat()
		if (newScale != oldScaleX || newScale != oldScaleY) {
			Log.debug("Window scale changed to: $newScale")
			scaleX = newScale
			scaleY = newScale
			sizeIsDirty = true
			_scaleChanged.dispatch(newScale, newScale)
		}
	}

	// TODO: Study context loss
	// getExtension( 'WEBGL_lose_context' ).loseContext();
	private fun webGlContextRestoredHandler() {
		Log.info("WebGL context lost")
		resizeHandler()
	}

	private fun blurHandler() {
		isActive = false
	}

	private fun focusHandler() {
		isActive = true
	}

	private fun fullScreenChangedHandler() {
		_fullScreenChanged.dispatch()
	}

	private fun resizeHandler() {
		setSizeInternal(canvas.offsetWidth.toFloat(), canvas.offsetHeight.toFloat(), isUserInteraction = true)
	}

	private fun watchForVisibilityChanges() {
		hiddenProp = null
		if (js("'hidden' in document")) {
			hiddenProp = "hidden"
		} else if (js("'mozHidden' in document")) {
			hiddenProp = "mozHidden"
		} else if (js("'webkitHidden' in document")) {
			hiddenProp = "webkitHidden"
		} else if (js("'msHidden' in document")) {
			hiddenProp = "msHidden"
		}
		if (hiddenProp != null) {
			document.addEventListener(hiddenPropEventMap[hiddenProp!!]!!, ::visibilityChangeHandler.as1)
			visibilityChangeHandler()
		}
	}

	private fun unwatchVisibilityChanges() {
		if (hiddenProp != null) {
			document.removeEventListener(hiddenPropEventMap[hiddenProp!!]!!, ::visibilityChangeHandler.as1)
			hiddenProp = null
		}
	}

	private fun visibilityChangeHandler() {
		isVisible = document[hiddenProp!!] != true
	}

	override var isVisible: Boolean = true
		private set(value) {
			if (field == value) return
			field = value
			_isVisibleChanged.dispatch(value)
		}

	override var isActive: Boolean = true
		private set(value) {
			if (field == value) return
			field = value
			_isActiveChanged.dispatch(value)
		}

	override val width: Float
		get() {
			return _width
		}

	override val height: Float
		get() {
			return _height
		}

	override var scaleX: Float = window.devicePixelRatio.toFloat()
		private set

	override var scaleY: Float = window.devicePixelRatio.toFloat()
		private set

	override fun setSize(width: Float, height: Float) = setSizeInternal(width, height, false)

	private fun setSizeInternal(width: Float, height: Float, isUserInteraction: Boolean) {
		if (_width == width && _height == height) return // no-op
		_width = width
		_height = height
		if (!isUserInteraction) {
			canvas.style.width = "${_width.toInt()}px"
			canvas.style.height = "${_height.toInt()}px"
		}
		sizeIsDirty = true
		_sizeChanged.dispatch(_width, _height, isUserInteraction)
		requestRender()
	}

	override var continuousRendering: Boolean = false
	private var _renderRequested: Boolean = true

	override fun shouldRender(clearRenderRequest: Boolean): Boolean {
		val bool = continuousRendering || _renderRequested
		if (clearRenderRequest && _renderRequested) _renderRequested = false
		return bool
	}

	override fun requestRender() {
		if (_renderRequested) return
		_renderRequested = true
	}

	override fun renderBegin() {
		if (sizeIsDirty) {
			sizeIsDirty = false
			canvas.width = (_width * scaleX).toInt()
			canvas.height = (_height * scaleY).toInt()
		}
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
	}

	override fun renderEnd() {
	}

	override fun isCloseRequested(): Boolean = false

	// TODO: Implement. Pop-ups can be closed
	override fun requestClose() {
	}

	private val _fullScreenChanged = Signal0()
	override val fullScreenChanged = _fullScreenChanged.asRo()

	override val fullScreenEnabled: Boolean
		get() = document.fullscreenEnabled

	override var fullScreen: Boolean
		get() = document.fullscreen
		set(value) {
			if (value && !fullScreen && document.fullscreenEnabled) {
				canvas.requestFullscreen().then { resizeHandler() }
			} else if (!value && fullScreen) {
				document.exitFullscreen().then { resizeHandler() }
			}
			requestRender()
		}

	private val _location by lazy { JsLocation(window.location) }
	override val location: Location
		get() = _location

	override fun alert(message: String) {
		window.alert(message)
	}

	override fun dispose() {
		_scaleChanged.dispose()
		_sizeChanged.dispose()
		_isVisibleChanged.dispose()
		scaleQuery.removeListener(::scaleChangedHandler.as1)

		window.removeEventListener("resize", ::resizeHandler.as1)
		canvas.removeEventListener("webglcontextlost", ::webGlContextRestoredHandler.as1)
		window.removeEventListener("blur", ::blurHandler.as1)
		window.removeEventListener("focus", ::focusHandler.as1)
		document.removeEventListener("fullscreenchange", ::fullScreenChangedHandler.as1)
		unwatchVisibilityChanges()
	}
}
