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

package com.acornui.recycle

import com.acornui.collection.copy
import com.acornui.EqualityCheck

/**
 * Recycles a list of item renderers, creating or disposing renderers only when there is no data match.
 *
 * The [factory] and [disposer] methods may use an [com.acornui.recycle.ObjectPool] to handle the case where there
 * is no data match, but there would still like to be recycling.
 *
 * @param data The updated list of data items.
 * @param existingElements The stale list of item renderers. This will be modified to reflect the new item renderers.
 * @param factory Used to create new item renderers as needed. [configure] will be called after factory to configure
 * the new element.
 * @param configure Used to configure the element.
 * @param disposer Used to dispose the element.
 * @param retriever Returns the current data value for the given element.
 * @param equality If set, uses custom equality rules. This guides how to know whether an item can be recycled or not.
 */
fun <E, T> recycle(
		data: Iterable<E>?,
		existingElements: MutableList<T>,
		factory: (item: E, index: Int) -> T,
		configure: (element: T, item: E, index: Int) -> Unit,
		disposer: (element: T) -> Unit,
		retriever: (element: T) -> E?,
		equality: EqualityCheck<E?> = { a, b -> a == b }
) {

	// Dispose items not found in the new data list first, so that the disposer can potentially pool those elements to
	// be retrieved again immediately in the factory.
	val remainingData = data?.toMutableList()
	val toRecycle = existingElements.copy()
	val iterator = toRecycle.iterator()
	while (iterator.hasNext()) {
		val next = iterator.next()
		val index = remainingData?.indexOfFirst { equality(retriever(next), it) } ?: -1
		if (index == -1) {
			disposer(next)
			iterator.remove()
		} else {
			remainingData?.removeAt(index)
		}
	}

	existingElements.clear()
	data?.forEachIndexed {
		i, item ->
		val foundIndex = toRecycle.indexOfFirst { equality(retriever(it), item) }
		val element = if (foundIndex == -1) {
			factory(item, i)
		} else {
			toRecycle.removeAt(foundIndex)
		}
		configure(element, item, i)
		existingElements.add(element)
	}
}

@Deprecated("Use mapToRecycled", ReplaceWith("data.mapToRecycled(other, factory, compare)"))
fun <E1, E2> recycle(data: List<E1>?, other: MutableList<E2>, factory: (E1) -> E2, compare: (E2, E1) -> Boolean) {
	data.mapToRecycled(other, factory, compare)
}

/**
 * Recycles elements from this list into [other] list. It is similar to [List.mapTo] except reuses elements that can
 * be found as a match using [compare].
 * @param other The list of transformed elements. This will be mutated to contain the new elements.
 * @param factory When there is no match via [compare], a new element will be constructed with this method.
 * @param compare Returns true when the output [R] should be recycled from the input [T].
 */
fun <T, R> List<T>?.mapToRecycled(other: MutableList<R>, factory: (T) -> R, compare: (R, T) -> Boolean) {
	recycle(
			this,
			other,
			factory = { e, index -> factory(e) },
			configure = { e1, e2, index -> },
			disposer = {},
			retriever = { element -> this?.find { compare(element, it) } }
	)
}

