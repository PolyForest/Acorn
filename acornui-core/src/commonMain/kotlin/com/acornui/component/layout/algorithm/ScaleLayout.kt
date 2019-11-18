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

package com.acornui.component.layout.algorithm

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.component.layout.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Owned
import com.acornui.graphic.Scaling
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.math.Vector2
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.math.floor

/**
 * This layout will scale and position its elements to fit the provided dimensions of the box.
 */
class ScaleLayout : LayoutAlgorithm<ScaleLayoutStyle, ScaleLayoutData> {

	override val style = ScaleLayoutStyle()

	override fun createLayoutData() = ScaleLayoutData()

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		if (elements.isEmpty()) return
		val size = Vector2.obtain()
		val padding = style.padding

		val childAvailableWidth: Float? = padding.reduceWidth(explicitWidth)
		val childAvailableHeight: Float? = padding.reduceHeight(explicitHeight)

		for (i in 0..elements.lastIndex) {
			val child = elements[i]
			val layoutData = child.layoutDataCast
			child.setSize(layoutData?.getPreferredWidth(childAvailableWidth), layoutData?.getPreferredHeight(childAvailableHeight))
			val w = maxOf(1f, child.width) // Don't allow the width/height to be 0 for divide by zero reasons.
			val h = maxOf(1f, child.height)

			val scaling = layoutData?.scaling ?: style.scaling
			scaling.apply(w, h, childAvailableWidth ?: w, childAvailableHeight ?: h, size)

			var scaleX = size.x / w
			var scaleY = size.y / h
			if (layoutData != null) {
				if (layoutData.minScaleX != null) scaleX = maxOf(scaleX, layoutData.minScaleX!!)
				if (layoutData.minScaleY != null) scaleY = maxOf(scaleY, layoutData.minScaleY!!)
				if (layoutData.maxScaleX != null) scaleX = minOf(scaleX, layoutData.maxScaleX!!)
				if (layoutData.maxScaleY != null) scaleY = minOf(scaleY, layoutData.maxScaleY!!)
			}
			child.setScaling(scaleX, scaleY, maxOf(scaleX, scaleY))
			child.moveTo(padding.left, padding.top)

			if (explicitWidth != null) {
				val remainingSpace = childAvailableWidth!! - w * scaleX
				if (remainingSpace != 0f) {
					when (layoutData?.horizontalAlign ?: style.horizontalAlign) {
						HAlign.LEFT -> {
						}
						HAlign.CENTER -> {
							val halfSpace = floor((remainingSpace * 0.5f))
							child.x = halfSpace + padding.left
						}
						HAlign.RIGHT -> {
							child.x = remainingSpace + padding.left
						}
					}
				}
			}
			if (explicitHeight != null) {
				val remainingSpace = childAvailableHeight!! - h * scaleY
				if (remainingSpace != 0f) {
					when (layoutData?.verticalAlign ?: style.verticalAlign) {
						VAlign.TOP -> {
						}
						VAlign.MIDDLE -> {
							val halfSpace = floor((remainingSpace * 0.5f))
							child.y = halfSpace + padding.top
						}
						VAlign.BASELINE, VAlign.BOTTOM -> {
							child.y = remainingSpace + padding.top
						}
					}
				}
			}
			if (explicitWidth == null) out.width = maxOf(out.width, size.x + padding.left + padding.right)
			if (explicitHeight == null) out.height = maxOf(out.height, size.y + padding.top + padding.bottom)
			out.baseline = out.height
		}
		Vector2.free(size)
	}
}

open class ScaleLayoutStyle : StyleBase() {

	override val type = Companion

	/**
	 * The Padding object with bottom, right, left, and top padding.
	 */
	var padding: PadRo by prop(Pad())
	var verticalAlign by prop(VAlign.MIDDLE)
	var horizontalAlign by prop(HAlign.CENTER)

	var scaling by prop(Scaling.FIT)

	companion object : StyleType<ScaleLayoutStyle>
}

open class ScaleLayoutData : BasicLayoutData() {

	var minScaleX: Float? by bindable(null)
	var minScaleY: Float? by bindable(null)
	var maxScaleX: Float? by bindable(null)
	var maxScaleY: Float? by bindable(null)

	/**
	 * If set, the vertical align of this element overrides the default of the scale layout algorithm.
	 */
	var verticalAlign: VAlign? by bindable(null)

	/**
	 * If set, the horizontal align of this element overrides the default of the scale layout algorithm.
	 */
	var horizontalAlign: HAlign? by bindable(null)

	var scaling: Scaling? by bindable(null)

	fun center() {
		verticalAlign = VAlign.MIDDLE
		horizontalAlign = HAlign.CENTER
	}
}

open class ScaleBoxLayoutContainer<E : UiComponent>(owner: Owned) : ElementLayoutContainer<ScaleLayoutStyle, ScaleLayoutData, E>(owner, ScaleLayout())

@Deprecated("Renamed to scaleGroup", ReplaceWith("this.scaleGroup(init)"), DeprecationLevel.ERROR)
@JvmName("scaleBoxT")
inline fun <E : UiComponent> Owned.scaleBox(init: ComponentInit<ScaleBoxLayoutContainer<E>> = {}): ScaleBoxLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return scaleGroup(init)
}

@Deprecated("Renamed to scaleGroup", ReplaceWith("this.scaleGroup(init)"), DeprecationLevel.ERROR)
inline fun Owned.scaleBox(init: ComponentInit<ScaleBoxLayoutContainer<UiComponent>> = {}): ScaleBoxLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return scaleGroup<UiComponent>(init)
}

@JvmName("scaleGroupT")
inline fun <E : UiComponent> Owned.scaleGroup(init: ComponentInit<ScaleBoxLayoutContainer<E>> = {}): ScaleBoxLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ScaleBoxLayoutContainer<E>(this).apply(init)
}

inline fun Owned.scaleGroup(init: ComponentInit<ScaleBoxLayoutContainer<UiComponent>> = {}): ScaleBoxLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return scaleGroup<UiComponent>(init)
}


