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

@file:Suppress("CssUnresolvedCustomProperty", "MemberVisibilityCanBePrivate")

package com.acornui.component

import com.acornui.Node
import com.acornui.component.style.CommonStyleTags
import com.acornui.component.style.CommonStyleTags.toggled
import com.acornui.component.style.cssClass
import com.acornui.css.px
import com.acornui.di.Context
import com.acornui.dom.a
import com.acornui.dom.addStyleToHead
import com.acornui.dom.div
import com.acornui.formatters.StringFormatter
import com.acornui.google.Icons
import com.acornui.input.clicked
import com.acornui.math.Easing
import com.acornui.math.lerp
import com.acornui.properties.afterChange
import com.acornui.recycle.recycle
import com.acornui.signal.*
import com.acornui.skins.CssProps
import com.acornui.tween.tween
import org.w3c.dom.events.MouseEvent
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.seconds

open class Tree<T : Node>(owner: Context, initialData: T) : Div(owner) {

	/**
	 * Dispatched when a descendant tree is added based on the [data].
	 * @see all
	 */
	val subTreeCreated = signal<Tree<T>>()

	private val subTrees = ArrayList<Tree<T>>()

	/**
	 * A container div which allows for relative positioning.
	 */
	private val inner = addChild(div {
		addClass(TreeStyle.inner)
	})

	/**
	 * The tree node's label.
	 */
	val labelComponent = inner.addElement(a {
		addClass(TreeStyle.label)
		clicked.listen {
			if (data.children.isNotEmpty()) {
				toggled = !toggled
			}
		}
	})

	/**
	 * Dispatched when [labelComponent] is clicked, and the node's children are empty.
	 */
	val leafClicked: Signal<MouseEvent>
		get() = labelComponent.clicked.filtered {
			data.children.isEmpty()
		}

	/**
	 * The div containing the child trees.
	 */
	val subTreesContainer = inner.addElement(div {
		addClass(TreeStyle.subTreesContainer)
	})

	/**
	 * Returns true if the dom contains the class [CommonStyleTags.toggled].
	 */
	var toggled: Boolean = false
		set(value) {
			if (field == value) return
			field = value
			inner.toggleClass(CommonStyleTags.toggled)

			subTreesContainer.apply {
				val from = if (value) 0.0 else 1.0
				val to = if (value) 1.0 else 0.0
				style.setProperty("overflow", "hidden")
				tween(0.3.seconds, Easing.pow2) { _, alpha ->
					val h = dom.scrollHeight
					style.setProperty("max-height", (lerp(from, to, alpha) * h).px.toString())
				}.start().completed.once {
					if (value) {
						style.removeProperty("max-height")
						style.removeProperty("overflow")
					}

				}
			}

		}

	class DataChangeEvent<T>(val oldData: T, val newData: T)

	/**
	 * This tree's [data] has changed.
	 */
	val dataChanged = signal<DataChangeEvent<T>>()

	/**
	 * This tree's node data.
	 * When this data is changed, a [dataChanged] signal will be dispatched.
	 *
	 * @see bindData
	 */
	var data: T = initialData
		set(value) {
			if (subTreeCreated.isDispatching) throw IllegalStateException("May not set data during a subTreeCreated signal.")
			val old = field
			if (old == value) return
			field = value
			dataChanged.dispatch(DataChangeEvent(old, value))
		}

	/**
	 * Invoked when the data has changed, and immediately with the new data.
	 */
	fun bindData(callback: (T) -> Unit): SignalSubscription {
		val data = data
		callback(data)
		return dataChanged.listen {
			callback(data)
		}
	}

	/**
	 * Invokes the callback on this tree node and all its descendants.
	 * This is a pre-order walk.
	 */
	fun allCurrent(callback: (Tree<T>) -> Unit) {
		callback(this)
		subTrees.forEach {
			it.allCurrent(callback)
		}
	}

	/**
	 * Invokes the callback on this tree node and all its current and future descendants.
	 * This is a pre-order walk.
	 */
	fun all(callback: Tree<T>.() -> Unit): SignalSubscription {
		allCurrent(callback)
		return subTreeCreated.listen {
			callback(it)
		}
	}

	private fun refreshChildren() {
		val data = data

		recycle(data.children.unsafeCast<List<T>>(), subTrees, factory = { item: T, _: Int ->
			val child = createChild(item)
			child.subTreeCreated.listen {
				// Bubble subTreeCreated events.
				subTreeCreated.dispatch(it)
			}
			subTreeCreated.dispatch(child)
			child
		}, configure = { element: Tree<T>, item: T, index: Int ->
			element.data = item
			subTreesContainer.removeElement(element)
			subTreesContainer.addElement(index, element)
		}, disposer = {
			it.dispose()
		}, retriever = { element ->
			element.data
		})
	}

	protected open fun createChild(data: T): Tree<T> = tree(data)

	/**
	 * Sets the method to format a label from the data value.
	 * @see formatter
	 */
	var formatter: StringFormatter<T>? by afterChange(null) { refreshLabel() }

	private fun refreshLabel() {
		label = formatter?.format(data) ?: ""
	}

	/**
	 * Sets the data to label formatter.
	 *
	 * This should typically be set on each tree node using the [all] block.
	 * Example:
	 * ```
	 * +tree {
	 * 	all {
	 * 		formatter { data -> data?.label ?: "null" }
	 * 	}
	 * }
	 * ```
	 *
	 * Another way to do this, for example to add an icon to a node, would be to bind to data changes:
	 * ```
	 * +tree(data) {
	 *   all { // This tree node and all descendents
	 *     bindData { newData ->
	 *       labelComponent.apply {
	 *         clearElements(dispose = true)
	 *         +icon(newData.icon)
	 *         +span(newData.label)*
	 *       }
	 *     }
	 *   }
	 * }
	 * ```
	 *
	 */
	fun formatter(value: StringFormatter<T>) {
		formatter = value
	}

	/**
	 * Sets [org.w3c.dom.HTMLElement.innerText] property on [labelComponent].
	 */
	override var label: String
		get() = labelComponent.label
		set(value) {
			labelComponent.label = value
		}

	init {
		addClass(TreeStyle.tree)
		bindData {
			refreshLabel()
			if (it.children.isEmpty())
				inner.removeClass(TreeStyle.withChildren)
			else
				inner.addClass(TreeStyle.withChildren)
			refreshChildren()
		}
	}
}

object TreeStyle {

	val tree by cssClass()
	val inner by cssClass()
	val label by cssClass()
	val subTreesContainer by cssClass()
	val withChildren by cssClass()

	init {
		@Suppress("CssInvalidPropertyValue")
		addStyleToHead(
			"""
$tree {
	--vGap: 8px;
	/* Should line up with the rotated chevron point. */
	--indent: 8px;
	/* The width of the horizontal line. */
	--indent2: 8px;
}

$inner {
	position: relative;
}

$label {
	display: inline-flex;
	align-items: center;
	cursor: pointer;
	user-select: none;
	-webkit-user-select: none;
	-moz-user-select: none;
	-webkit-touch-callout: none;
	padding: ${CssProps.inputPadding.v};
}

$inner$withChildren > $label:before {
	content: "${Icons.CHEVRON_RIGHT.toChar()}";
	font-family: "Material Icons";
    display: inline-block;
    white-space: nowrap;
    -webkit-font-smoothing: antialiased;
	transition: transform 0.3s ease-out;
}

$inner$withChildren > $label {
	padding-left: 0;
	color: inherit;
}

$withChildren$toggled > $label:before {
	transform: rotate(90deg);
}

$subTreesContainer {
	margin-left: var(--indent);
	opacity: 0;
	transition: opacity 0.3s ease-out;
}

$toggled > $subTreesContainer {
	opacity: 1;
}

$subTreesContainer > $tree {
	padding-left: var(--indent2);
}

$subTreesContainer > $tree > $inner > $label {
	margin-top: var(--vGap);
}

$subTreesContainer > $tree:not(:last-child) {
	border-left: 1px solid #777676;
}

$subTreesContainer > $tree:last-child > $inner:before {
	border-left: 1px solid #777676;
}

$subTreesContainer > $tree > $inner:before {
	content: " ";
	position: absolute;
	left: calc(var(--indent2) * -1);
	top: 0;
	border-bottom: 1px solid #777676;
	width: calc(var(--indent2) - 2px);
	height: calc(var(--vGap) + ${CssProps.inputPadding.v} + 0.6em);
}
			"""
		)
	}
}

inline fun <T : Node> Context.tree(initialData: T, init: ComponentInit<Tree<T>> = {}): Tree<T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Tree(this, initialData).apply(init)
}