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

package com.acornui.io

import com.acornui.collection.addOrSet

import com.acornui.core.*

/**
 * A Buffer interface wrapping an [ArrayList]
 * @author nbilyk
 */
class ArrayListBuffer<T>(private val array: ArrayList<T>, private val arrayOffset:Int = 0, capacity:Int = INT_MAX_VALUE) : BufferBase<T>(capacity), ReadWriteBuffer<T> {

	override fun get(): T {
		if (_position >= _limit) {
			throw BufferUnderflowException()
		}
		return array[arrayOffset + _position++]
	}

	override fun put(value: T) {
		if (_position >= _limit) {
			throw BufferOverflowException()
		}
		array.addOrSet(arrayOffset + _position++, value)
	}
}