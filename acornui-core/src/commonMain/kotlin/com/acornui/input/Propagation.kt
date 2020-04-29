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

package com.acornui.input

import com.acornui.recycle.Clearable

interface PropagationRo {

	val immediatePropagationStopped: Boolean

	/**
	 * Immediately stops execution of the sequence, neither progressing further, or finishing the current level.
	 */
	fun stopImmediatePropagation()

	val propagationStopped: Boolean

	/**
	 * Stops the sequence from going to the next level, but will continue along the current level.
	 */
	fun stopPropagation()
}

/**
 * Given a ladder of propagation, [stopPropagation] stops the sequence from reaching the next rung, and
 * [stopImmediatePropagation] stops the sequence from reaching the next rung and stops the sequence from continuing
 * on the current rung.
 *
 * @author nbilyk
 */
class Propagation : PropagationRo, Clearable {

	override var immediatePropagationStopped: Boolean = false
		private set

	/**
	 * Immediately stops execution of the sequence, neither progressing further, or finishing the current level.
	 */
	override fun stopImmediatePropagation() {
		immediatePropagationStopped = true
		propagationStopped = true
	}

	override var propagationStopped: Boolean = false
		private set

	/**
	 * Stops the sequence from going to the next level, but will continue along the current level.
	 */
	override fun stopPropagation() {
		propagationStopped = true
	}

	override fun clear() {
		immediatePropagationStopped = false
		propagationStopped = false
	}
}
