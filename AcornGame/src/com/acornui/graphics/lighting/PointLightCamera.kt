package com.acornui.graphics.lighting

import com.acornui.core.graphics.PerspectiveCamera
import com.acornui.core.graphics.Window
import com.acornui.math.MathUtils
import com.acornui.math.Vector3

/**
 * @author nbilyk
 */
class PointLightCamera(window: Window, val resolution: Float) {

	val camera = PerspectiveCamera(window)

	init {
		camera.autoCenter = false
		camera.fieldOfView = 90f * MathUtils.degRad
		camera.viewportWidth = resolution
		camera.viewportHeight = resolution
	}

	fun update(pointLight: PointLight, direction: Int) {
		if (pointLight.radius < 0.0001f) return
		camera.position.set(pointLight.position)
		camera.direction.set(CUBEMAP_DIRECTIONS[direction])
		camera.up.set(CUBEMAP_UP[direction])
		camera.update()
	}

	companion object {
		// positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ
		private val CUBEMAP_DIRECTIONS = arrayOf(Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, -1f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
		private val CUBEMAP_UP = arrayOf(Vector3(0f, -1f, 0f), Vector3(0f, -1f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f), Vector3(0f, -1f, 0f), Vector3(0f, -1f, 0f))
	}
}
