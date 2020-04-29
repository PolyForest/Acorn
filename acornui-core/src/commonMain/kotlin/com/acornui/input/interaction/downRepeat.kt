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

package com.acornui.input.interaction

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.input.*
import com.acornui.time.nowMs
import com.acornui.time.timer
import kotlin.time.Duration
import kotlin.time.seconds

class DownRepeat(
		private val target: UiComponentRo
) : ContextImpl(target) {

	private val mouseState = inject(MouseState)
	private val stage = target.stage
	private val interactivity = inject(InteractivityManager)

	val style = target.bind(DownRepeatStyle())

	private var repeatTimer: Disposable? = null

	private val mouseDownRepeat = MouseInteraction()

	private fun mouseRepeatHandler() {
		if (!target.mouseIsOver()) return
		val e = mouseDownRepeat
		e.clear()
		e.type = MouseInteractionRo.MOUSE_DOWN
		e.isFabricated = true
		e.canvasX = mouseState.mouseX
		e.canvasY = mouseState.mouseY
		e.button = WhichButton.LEFT
		e.timestamp = nowMs()
		e.currentTarget = target
		interactivity.dispatch(e.canvasX, e.canvasY, e)
	}

	private fun mouseDownHandler(event: MouseInteractionRo) {
		if (event !== mouseDownRepeat) {
			repeatTimer?.dispose()
			repeatTimer = timer(style.repeatInterval, -1, style.repeatDelay, ::mouseRepeatHandler.as1)
			stage.mouseUp().add(::rawMouseUpHandler, true)
		}
	}

	private fun rawMouseUpHandler(event: MouseInteractionRo) {
		if (event.button == WhichButton.LEFT) {
			repeatTimer?.dispose()
			repeatTimer = null
		}
	}

	init {
		target.mouseDown().add(::mouseDownHandler)
	}

	override fun dispose() {
		super.dispose()
		target.unbind(DownRepeatStyle())
		target.mouseDown().remove(::mouseDownHandler)
		stage.mouseUp().remove(::rawMouseUpHandler)
		repeatTimer?.dispose()
		repeatTimer = null
	}

	companion object
}

/**
 * Returns true if the down repeat interaction is enabled on this [UiComponent].
 */
fun UiComponentRo.downRepeatEnabled(): Boolean {
	return getAttachment<DownRepeat>(DownRepeat) != null
}

fun UiComponentRo.enableDownRepeat(): DownRepeat {
	return createOrReuseAttachment(DownRepeat) { DownRepeat(this) }
}

/**
 * Sets this component to dispatch a mouse down event repeatedly after holding down on the target.
 * @param repeatDelay The number of seconds after holding down the target to start repeat dispatching.
 * @param repeatInterval Once the repeat dispatching begins, subsequent events are dispatched at this interval (in
 * seconds).
 */
fun UiComponentRo.enableDownRepeat(repeatDelay: Duration, repeatInterval: Duration): DownRepeat {
	return createOrReuseAttachment(DownRepeat) {
		val dR = DownRepeat(this)
		dR.style.repeatDelay = repeatDelay
		dR.style.repeatInterval = repeatInterval
		dR
	}
}

fun UiComponentRo.disableDownRepeat() {
	removeAttachment<DownRepeat>(DownRepeat)?.dispose()
}

class DownRepeatStyle : StyleBase() {
	override val type = Companion

	var repeatDelay by prop(0.24.seconds)
	var repeatInterval by prop(0.02.seconds)

	companion object : StyleType<DownRepeatStyle>
}
