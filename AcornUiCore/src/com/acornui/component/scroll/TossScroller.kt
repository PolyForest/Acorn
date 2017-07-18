/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.component.scroll

import com.acornui.collection.poll
import com.acornui.component.InteractiveElement
import com.acornui.component.UiComponent
import com.acornui.component.createOrReuseAttachment
import com.acornui.core.AppConfig
import com.acornui.core.Disposable
import com.acornui.core.di.inject
import com.acornui.core.input.InteractionType
import com.acornui.core.input.interaction.*
import com.acornui.core.time.EnterFrame
import com.acornui.core.time.enterFrame
import com.acornui.core.time.time
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro
import com.acornui.math.Vector2
import com.acornui.signal.StoppableSignal
import com.acornui.signal.StoppableSignalImpl


/**
 * A toss scroller lets you grab a target component, and update [ScrollModel] objects by dragging it.
 */
class TossScroller(
		val target: UiComponent,

		/**
		 * Dampening affects how quickly the toss velocity will slow to a stop.
		 * Make this number 0 &lt; dampening &lt; 1.  Where 1 will go forever, and 0 will prevent any momentum.
		 */
		var dampening: Float = TossScroller.DEFAULT_DAMPENING,

		val dragAttachment: Drag = target.dragAttachment(TossScroller.minTossDistance)
) : Disposable {

	private val stepTime = target.inject(AppConfig).stepTime

	private val _tossStart = StoppableSignalImpl<DragInteraction>()

	/**
	 *  Dispatched when the toss has begun.
	 */
	val tossStart: StoppableSignal<DragInteraction>
		get() = _tossStart

	private val _toss = StoppableSignalImpl<DragInteraction>()

	/**
	 *  Dispatched every frame the toss is being updated.
	 */
	val toss: StoppableSignal<DragInteraction>
		get() = _toss

	private val _tossEnd = StoppableSignalImpl<DragInteraction>()

	/**
	 * Dispatched when the toss has ended.
	 */
	val tossEnd: StoppableSignal<DragInteraction>
		get() = _tossEnd

	private val velocity = Vector2()

	private val event = DragInteraction()
	private var startElement: InteractiveElement? = null
	private val startPosition: Vector2 = Vector2()
	private val position: Vector2 = Vector2()

	private val historyPoints = ArrayList<Vector2>()
	private val historyTimes = ArrayList<Long>()

	private var _timer: EnterFrame? = null
	private var clickPreventer = 0

	private val diff = Vector2()

	private val dragStartHandler = {
		event: DragInteraction ->
		stop()
		startPosition.set(event.startPosition)
		position.set(event.position)
		startElement = event.startElement

		clickPreventer = 5
		clearHistory()
		pushHistory()
		startEnterFrame()
		dispatchDragEvent(TOSS_START, _tossStart)
		Unit
	}

	private fun pushHistory() {
		historyPoints.add(Vector2.obtain().set(position.x, position.y))
		historyTimes.add(time.nowMs())
		if (historyPoints.size > MAX_HISTORY) {
			historyPoints.poll().free()
			historyTimes.poll()
		}
	}

	private fun startEnterFrame() {
		if (_timer == null) {
			_timer = target.enterFrame {
				if (dragAttachment.isDragging) {
					// History is also added in an enter frame instead of the drag handler so that if the user stops dragging, the history reflects that.
					pushHistory()
				} else {
					if (clickPreventer > 0) clickPreventer--
					velocity.scl(dampening)
					position.add(velocity)
					dispatchDragEvent(TOSS, _toss)
					if (velocity.isZero(0.1f)) {
						stop()
					}
				}
			}
		}
	}

	private val dragHandler = {
		event: DragInteraction ->
		position.set(event.position)
		dispatchDragEvent(TOSS, _toss)
		Unit
	}

	private fun dispatchDragEvent(type: InteractionType<DragInteraction>, signal: StoppableSignalImpl<DragInteraction>) {
		event.clear()
		event.type = type
		event.startElement = startElement
		event.startPosition.set(startPosition)
		event.position.set(position)
		signal.dispatch(event)
	}

	private val dragEndHandler = {
		event: DragInteraction ->
		pushHistory()
		// Calculate the velocity.
		if (historyPoints.size >= 2) {
			diff.set(historyPoints.last()).sub(historyPoints.first())
			val time = (historyTimes.last() - historyTimes.first()) * 0.001f
			velocity.set(diff.x / time, diff.y / time).scl(stepTime)
		}
		clearHistory()
	}

	private fun clearHistory() {
		for (i in 0..historyPoints.lastIndex) {
			historyPoints[i].free()
		}
		historyPoints.clear()
		historyTimes.clear()
	}

	private val clickHandler = {
		event: ClickInteraction ->
		if (clickPreventer > 0) {
			event.propagation.stopImmediatePropagation()
		}
	}

	init {
		dragAttachment.dragStart.add(dragStartHandler)
		dragAttachment.drag.add(dragHandler)
		dragAttachment.dragEnd.add(dragEndHandler)
		target.click(isCapture = true).add(clickHandler)
	}

	/**
	 * Enables or disables this toss scroller. If this is set to false in the middle of a toss, the toss will
	 * still finish as if the drag were immediately released. Use [stop] to halt the current velocity.
	 */
	var enabled: Boolean
		get() = dragAttachment.enabled
		set(value) {
			dragAttachment.enabled = value
		}

	fun stop() {
		if (!velocity.isZero() || _timer != null) {
			dispatchDragEvent(TOSS_END, _tossEnd)
			clickPreventer = 0
			velocity.clear()
			_timer?.stop()
			_timer = null
			startElement = null
			event.clear()
		}
	}

	override fun dispose() {
		stop()
		_tossStart.dispose()
		_toss.dispose()
		_tossEnd.dispose()
		dragAttachment.dragStart.remove(dragStartHandler)
		dragAttachment.drag.remove(dragHandler)
		dragAttachment.dragEnd.remove(dragEndHandler)
		target.click(isCapture = true).remove(clickHandler)
	}

	companion object {

		val TOSS_START = InteractionType<DragInteraction>("tossStart")
		val TOSS = InteractionType<DragInteraction>("toss")
		val TOSS_END = InteractionType<DragInteraction>("tossEnd")

		val DEFAULT_DAMPENING: Float = 0.9f
		private val MAX_HISTORY = 15

		var minTossDistance: Float = 7f
	}
}

class TossScrollModelBinding(
		val tossScroller: TossScroller,
		val hScrollModel: MutableScrollModel,
		val vScrollModel: MutableScrollModel
) : Disposable {

	var modelToPixelsX: Float = 1f
	var modelToPixelsY: Float = 1f

	private val modelStart = Vector2()
	private val diffPixels = Vector2()

	private var matrix: Matrix4Ro? = null

	private val tossStartHandler = {
		_: DragInteraction ->
		modelStart.set(hScrollModel.value, vScrollModel.value)
		matrix = tossScroller.target.concatenatedTransformInv
	}

	private val changedHandler = {
		event: DragInteraction ->
		val cM = matrix!!
		diffPixels.set(event.startPosition).sub(event.position)
		cM.rot(diffPixels)

		hScrollModel.value = modelStart.x + diffPixels.x / modelToPixelsX
		vScrollModel.value = modelStart.y + diffPixels.y / modelToPixelsY

		Unit
	}

	init {
		tossScroller.tossStart.add(tossStartHandler)
		tossScroller.toss.add(changedHandler)
	}

	override fun dispose() {
		tossScroller.tossStart.remove(tossStartHandler)
		tossScroller.toss.remove(changedHandler)
		matrix = null
	}
}

fun UiComponent.enableTossScrolling(dampening: Float = TossScroller.DEFAULT_DAMPENING): TossScroller {
	return createOrReuseAttachment(TossScroller, { TossScroller(this, dampening) })
}

fun UiComponent.disableTossScrolling() {
	removeAttachment<TossScroller>(TossScroller)?.dispose()
}