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

package com.acornui.time

import com.acornui.Disposable
import com.acornui.UpdatableChild
import com.acornui.UpdatableChildBase
import com.acornui.di.Scoped
import com.acornui.di.inject

interface CallbackWrapper : Disposable {
	operator fun invoke()
}

internal class LimitedCallback(
		val timeDriver: TimeDriver,
		val duration: Float,
		val callback: () -> Unit
) : UpdatableChildBase(), UpdatableChild, CallbackWrapper {

	private var currentTime = 0f
	private var pendingInvoke = false
	private var isInvoking = false

	override fun update(tickTime: Float) {
		currentTime += tickTime
		if (currentTime > duration) {
			currentTime = 0f
			if (pendingInvoke) {
				pendingInvoke = false
				isInvoking = true
				callback()
				isInvoking = false
			} else {
				remove()
			}
		}
	}

	override operator fun invoke() {
		if (isInvoking) return
		if (parent == null) {
			timeDriver.addChild(this)
			callback()
		} else {
			pendingInvoke = true
		}
	}

	override fun dispose() {
		remove()
	}
}

/**
 * Creates a callback wrapper that will prevent a method from being invoked too rapidly.
 * @param duration When the wrapper is invoked, a timed lock will be created for this number of seconds. If the wrapper
 * is invoked while this timed lock is in place, the callback will be invoked at the end of the lock.
 * Note that calls to the wrapper from within the callback will be ignored.
 */
fun Scoped.limitedCallback(duration: Float, callback: () -> Unit): CallbackWrapper {
	return LimitedCallback(inject(TimeDriver), duration, callback)
}

internal class DelayedCallback(
		val timeDriver: TimeDriver,
		val duration: Float,
		val callback: () -> Unit
) : UpdatableChildBase(), UpdatableChild, CallbackWrapper {

	private var currentTime: Float = 0f

	private var isInvoking = false

	override fun update(tickTime: Float) {
		currentTime += tickTime
		if (currentTime > duration) {
			currentTime = 0f
			remove()
			isInvoking = true
			callback()
			isInvoking = false
		}
	}

	override operator fun invoke() {
		if (isInvoking) return
		if (parent == null) {
			timeDriver.addChild(this)
		} else {
			currentTime = 0f
		}
	}

	override fun dispose() {
		remove()
	}
}

/**
 * Returns a wrapper that when invoked, will call [callback] after [duration] seconds. Every consecutive
 * invocation of the wrapper will reset this timer.
 * Useful for things like auto-save after the user is done typing for a set amount of time.
 *
 * Note that calls to the wrapper from within the callback will be ignored.
 *
 * @param duration The number of seconds before the [callback] is invoked.
 * @param callback The function to call after [duration] seconds.
 */
fun Scoped.delayedCallback(duration: Float, callback: () -> Unit): CallbackWrapper {
	return DelayedCallback(inject(TimeDriver), duration, callback)
}