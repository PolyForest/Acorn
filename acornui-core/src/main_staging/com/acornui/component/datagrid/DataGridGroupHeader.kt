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

package com.acornui.component.datagrid

import com.acornui.collection.ObservableList
import com.acornui.collection.addAll
import com.acornui.component.*
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.algorithm.HorizontalLayout
import com.acornui.component.layout.algorithm.HorizontalLayoutData
import com.acornui.component.layout.algorithm.HorizontalLayoutStyle
import com.acornui.component.layout.size
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.noSkinOptional
import com.acornui.component.text.TextStyleTags
import com.acornui.di.Context
import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface DataGridGroupHeader : UiComponent {

	var collapsed: Boolean

	companion object : StyleTag
}

open class DataGridGroupHeaderImpl<E>(
		owner: Context,
		protected val group: DataGridGroup<E>,
		protected val list: ObservableList<E>
) : ElementLayoutContainer<HorizontalLayoutStyle, HorizontalLayoutData, UiComponent>(
		owner,
		HorizontalLayout()
), DataGridGroupHeader, Labelable {

	private var background: UiComponent? = null
	private var collapseButton: ButtonImpl? = null

	val groupStyle = bind(DataGridGroupHeaderStyle())

	init {
		addClassAll(DataGridGroupHeader, TextStyleTags.large)
		interactivityMode = InteractivityMode.CHILDREN

		watch(groupStyle) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))
			background?.interactivityMode = InteractivityMode.NONE

			collapseButton?.dispose()
			collapseButton = addOptionalElement(0, it.collapseButton(this))
			collapseButton?.click()?.add {
				group.collapsed = !group.collapsed
			}
			collapseButton?.toggled = !_collapsed
			collapseButton?.label = _label
		}
	}

	private var _label = ""
	override var label: String
		get() = _label
		set(value) {
			_label = value
			collapseButton?.label = value
		}

	private var _collapsed = false
	override var collapsed: Boolean
		get() = _collapsed
		set(value) {
			collapseButton?.toggled = !value
		}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		super.updateLayout(explicitWidth, explicitHeight, out)
		background?.size(out)
	}
}

class DataGridGroupHeaderStyle : ObservableBase() {

	override val type: StyleType<DataGridGroupHeaderStyle> = Companion

	/**
	 * Added to group headers, when clicked, the group will be collapsed.
	 */
	var collapseButton by prop<Context.() -> ButtonImpl>({ throw Exception("Skin part must be created.") })

	/**
	 * The header background for groups.
	 */
	var background by prop(noSkinOptional)

	companion object : StyleType<DataGridGroupHeaderStyle>
}

fun <E> Context.dataGridGroupHeader(group: DataGridGroup<E>, list: ObservableList<E>, label: String = "", init: ComponentInit<DataGridGroupHeaderImpl<E>> = {}): DataGridGroupHeader {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val d = DataGridGroupHeaderImpl(this, group, list)
	d.label = label
	d.init()
	return d
}