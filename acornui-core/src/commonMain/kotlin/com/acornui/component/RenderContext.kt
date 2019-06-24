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

package com.acornui.component

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.CameraRo
import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.Window
import com.acornui.core.graphic.centerCamera
import com.acornui.function.as2
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*

interface RenderContextRo : CanvasTransformableRo {

	/**
	 * The clipping region, in canvas coordinates.
	 * This is used to early-out on rendering if the render contents is outside of this region.
	 * This is not done automatically; it is the responsibility of the component.
	 */
	val clipRegion: MinMaxRo

	/**
	 * The color multiplier for vertices.
	 */
	val colorTint: ColorRo

	/**
	 * Validates the properties of the render context.
	 */
	fun validate()

	companion object : DKey<RenderContextRo> {

		override fun factory(injector: Injector): RenderContextRo? = DefaultRenderContext(injector)
	}
}

/**
 * Returns true if the camera and viewport match.
 */
fun RenderContextRo.cameraEquals(renderContext: RenderContextRo): Boolean {
	return viewProjectionTransform == renderContext.viewProjectionTransform && canvasTransform == renderContext.canvasTransform
}

class DefaultRenderContext(override val injector: Injector) : Scoped, RenderContextRo, Disposable {

	private val camera = OrthographicCamera()
	private val window = inject(Window)

	private var _clipRegion = MinMax()
	private var _canvasTransform = Rectangle()

	private var isValid = false

	init {
		window.sizeChanged.add(::invalidate.as2)
		window.scaleChanged.add(::invalidate.as2)
	}

	private fun invalidate() {
		isValid = false
	}

	override val modelTransform: Matrix4Ro = Matrix4.IDENTITY
	override val modelTransformInv: Matrix4Ro = Matrix4.IDENTITY

	override val viewProjectionTransformInv: Matrix4Ro
		get() = camera.combinedInv

	override val viewProjectionTransform: Matrix4Ro
		get() = camera.combined

	override val viewTransform: Matrix4Ro
		get() = camera.view

	override val projectionTransform: Matrix4Ro
		get() = camera.projection

	override val clipRegion: MinMaxRo = _clipRegion

	override val canvasTransform: RectangleRo = _canvasTransform

	override val colorTint: ColorRo = Color.WHITE

	override fun validate() {
		if (isValid) return
		isValid = true
		val window = window
		val w = window.width
		val h = window.height
		window.centerCamera(camera)
		_clipRegion.set(0f, 0f, w, h)
		_canvasTransform.set(0f, 0f, w, h)
	}

	override fun dispose() {
		window.sizeChanged.remove(::invalidate.as2)
		window.scaleChanged.remove(::invalidate.as2)
	}
}

class RenderContext(initialParentContext: RenderContextRo) : RenderContextRo {

	var parentContext: RenderContextRo = initialParentContext

	var cameraOverride: CameraRo? = null
	var modelTransformLocal: Matrix4Ro = Matrix4.IDENTITY
	var canvasTransformOverride: RectangleRo? = null

	/**
	 * The clip region, in local coordinates.
	 * If set, this will transform to canvas coordinates and [clipRegion] will be the intersection of the
	 * parent clip region and this region.
	 */
	var clipRegionLocal: MinMaxRo? = null

	var colorTintLocal: ColorRo = Color.WHITE

	var modelTransformOverride: Matrix4Ro? = null

	var clipRegionOverride: MinMaxRo? = null

	private val _modelTransform = Matrix4()
	override val modelTransform: Matrix4Ro
		get() = modelTransformOverride ?: _modelTransform.set(parentContext.modelTransform).mul(modelTransformLocal)

	private val _concatenatedTransformInv = Matrix4()
	override val modelTransformInv: Matrix4Ro
		get() = _concatenatedTransformInv.set(modelTransform).inv()

	override val viewProjectionTransform: Matrix4Ro
		get() = cameraOverride?.combined ?: parentContext.viewProjectionTransform

	override val viewProjectionTransformInv: Matrix4Ro
		get() = cameraOverride?.combinedInv ?: parentContext.viewProjectionTransformInv

	override val viewTransform: Matrix4Ro
		get() = cameraOverride?.view ?: parentContext.viewTransform

	override val projectionTransform: Matrix4Ro
		get() = cameraOverride?.projection ?: parentContext.projectionTransform

	override val canvasTransform: RectangleRo
		get() {
			return canvasTransformOverride ?: parentContext.canvasTransform
		}

	private val _clipRegionIntersection: MinMax = MinMax()
	override val clipRegion: MinMaxRo
		get() {
			return clipRegionOverride ?: if (clipRegionLocal == null) parentContext.clipRegion
			else localToCanvas(_clipRegionIntersection.set(clipRegionLocal!!)).intersection(parentContext.clipRegion)
		}

	var colorTintOverride: ColorRo? = null

	private val _colorTint = Color()
	override val colorTint: ColorRo
		get() = colorTintOverride ?: _colorTint.set(parentContext.colorTint).mul(colorTintLocal).clamp()

	override fun validate() {
	}
}
