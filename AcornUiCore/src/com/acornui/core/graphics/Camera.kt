/*
* Derived from LibGDX by Nicholas Bilyk
* https://github.com/libgdx
* Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

package com.acornui.core.graphics

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.math.*
import com.acornui.observe.ModTag
import com.acornui.observe.ModTagImpl


interface Camera : Disposable {

	/**
	 * Incremented whenever something on this camera has changed.
	 */
	val modTag: ModTag

	/**
	 * The combined projection and view matrix.
	 * This is read only, and will be accurate after an [update]
	 */
	val combined: Matrix4Ro

	/**
	 * Creates a picking [Ray] from the coordinates given in screen coordinates. It is assumed that the viewport spans
	 * the whole canvas. The global coordinates origin is assumed to be in the top left corner, its y-axis pointing
	 * down, the x-axis pointing to the right.
	 * @return The [out] parameter. The [Ray] will be in global coordinate space.
	 */
	fun getPickRay(canvasX: Float, canvasY: Float, out: Ray): Ray

	/**
	 * Projects the {@link Vector3} given in global space to screen coordinates. It's the same as GLU gluProject
	 * with one small deviation: The viewport is assumed to span the whole screen. The screen coordinate system has
	 * its origin in the top left, with the y-axis pointing downwards and the x-axis pointing to the right.
	 */
	fun project(globalCoords: Vector3): Vector3

	/**
	 * The position of the camera
	 */
	val position: Vector3

	/**
	 * The unit length direction vector of the camera
	 */
	val direction: Vector3

	/**
	 * The unit length up vector of the camera
	 */
	val up: Vector3

	/**
	 * The projection matrix.
	 * This will be valid after [update].
	 */
	val projection: Matrix4Ro

	/**
	 * The view matrix.
	 * This will be valid after [update].
	 */
	val view: Matrix4Ro

	/**
	 * The near clipping plane distance, has to be positive
	 */
	var near: Float

	/**
	 * The far clipping plane distance, has to be positive
	 */
	var far: Float

	/**
	 * The viewport width
	 */
	var viewportWidth: Float

	/**
	 * The viewport height
	 */
	var viewportHeight: Float

	val aspect: Float
		get() = viewportWidth / viewportHeight

	/**
	 * The frustum
	 */
	val frustum: FrustumRo

	/**
	 * The inverse combined projection and view matrix
	 */
	val invCombined: Matrix4Ro

	/**
	 * If true (default) this camera will call [centerCamera] after the window has resized.
	 */
	var autoCenter: Boolean

	/**
	 * Centers the camera on the screen.
	 * [update] must be called after this.
	 */
	fun centerCamera()

	/**
	 * Recalculates the projection and view matrix of this camera and the Frustum planes if <code>updateFrustum</code>
	 * is true. Use this after you've manipulated any of the attributes of the camera.
	 *
	 * Implementations of this method should call [modTag.increment()] to track a change count.
	 * Observers of this camera can check if it's been updated by comparing a local modTag to this camera's modTag
	 */
	fun update(updateFrustum: Boolean = true)

	fun setViewport(width: Float, height: Float) {
		viewportWidth = maxOf(1f, width)
		viewportHeight = maxOf(1f, height)
	}

	fun setDirection(value: Vector3) = setDirection(value.x, value.y, value.z)

	/**
	 * Sets this camera's direction, ensuring that the up vector and direction vector remain orthonormal.
	 */
	fun setDirection(x: Float, y: Float, z: Float)

	fun pointToLookAt(target: Vector3) = pointToLookAt(target.x, target.y, target.z)

	/**
	 * Recalculates the direction of the camera to look at the point (x, y, z). This function assumes the up vector is
	 * normalized.
	 *
	 * @param x the x-coordinate of the point to look at
	 * @param y the x-coordinate of the point to look at
	 * @param z the x-coordinate of the point to look at
	 */
	fun pointToLookAt(x: Float, y: Float, z: Float)

	fun moveToLookAtPoint(x: Float, y: Float, z: Float, distance: Float = 1.0f)

	fun moveToLookAtRect(rect: Rectangle, scaling: Scaling = Scaling.FIT) = moveToLookAtRect(rect.x, rect.y, rect.width, rect.height, scaling)

	/**
	 * Moves and/or zooms the camera to fit the given 2d bounding box into the viewport, maintaining the current
	 * direction.
	 * @param scaling The scaling type to fit to the given width/height. This may not be a stretch type.
	 */
	fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling = Scaling.FIT)

	/**
	 * Function to translate a point given in screen coordinates to global space. It's the same as GLU gluUnProject but
	 * does not rely on OpenGL. The viewport is assumed to span the whole screen and is fetched from [Window.getWidth]
	 * and [Window.getHeight]. The x- and y-coordinate of [canvasCoords] are assumed to be in screen coordinates
	 * (origin is the top left corner, y pointing down, x pointing to the right) as reported by the canvas coordinates
	 * in input events. A z-coordinate of 0 will return a point on the near plane, a z-coordinate of 1 will return a
	 * point on the far plane.
	 * @param canvasCoords the point in screen coordinates
	 */
	fun canvasToGlobal(canvasCoords: Vector3): Vector3

	/**
	 * Translates a point given in screen coordinates to global space. It's the same as GLU gluUnProject, but
	 * does not rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the
	 * top left corner, y pointing down, x pointing to the right) as reported by the canvas coordinates in
	 * input events. A z-coordinate of 0 will return a point on the near plane, a z-coordinate
	 * of 1 will return a point on the far plane. This method allows you to specify the viewport position and
	 * dimensions in the coordinate system expected by {@link GL20#glViewport(int, int, int, int)}, with the origin in
	 * the top left corner of the screen.
	 * @param canvasCoords the point in canvas coordinates (origin top left)
	 * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportWidth the width of the viewport in pixels
	 * @param viewportHeight the height of the viewport in pixels
	 */
	fun canvasToGlobal(canvasCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3

	companion object : DKey<Camera>
}

abstract class CameraBase(private val window: Window) : Camera {

	protected val _combined: Matrix4 = Matrix4()
	override val combined: Matrix4Ro
		get() = _combined

	/**
	 * Incremented whenever something on this camera has changed.
	 */
	override val modTag: ModTagImpl = ModTagImpl()

	/**
	 * The position of the camera
	 */
	override val position: Vector3 = Vector3()

	/**
	 * The unit length direction vector of the camera
	 */
	override val direction: Vector3 = Vector3(0f, 0f, 1f)

	/**
	 * The unit length up vector of the camera
	 */
	override val up: Vector3 = Vector3(0f, -1f, 0f)

	protected val _projection: Matrix4 = Matrix4()

	/**
	 * The projection matrix
	 */
	override val projection: Matrix4Ro
		get() = _projection

	protected val _view: Matrix4 = Matrix4()

	/**
	 * The view matrix
	 */
	override val view: Matrix4Ro
		get() = _view

	/**
	 * The near clipping plane distance, has to be positive
	 */
	override var near: Float = 1f

	/**
	 * The far clipping plane distance, has to be positive
	 */
	override var far: Float = 3000f

	/**
	 * The viewport width
	 */
	override var viewportWidth: Float = 1f

	/**
	 * The viewport height
	 */
	override var viewportHeight: Float = 1f

	//------------------------------------
	// Set after update()
	//------------------------------------

	protected val _frustum: Frustum = Frustum()

	/**
	 * The frustum
	 */
	override val frustum: FrustumRo
		get() = _frustum

	protected val _invCombined: Matrix4 = Matrix4()

	/**
	 * The inverse combined projection and view matrix
	 */
	override val invCombined: Matrix4Ro
		get() = _invCombined

	private var _autoCenter: Boolean = true

	override var autoCenter: Boolean
		get() = _autoCenter
		set(value) {
			if (_autoCenter == value) return
			_autoCenter = value
			if (value) window.sizeChanged.add(windowResizedHandler)
			else window.sizeChanged.remove(windowResizedHandler)
		}

	//------------------------------------
	// Temp storage
	//------------------------------------

	private val tmpVec = Vector3()

	private val windowResizedHandler = {
		newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		centerCamera()
		update()
	}

	init {
		window.sizeChanged.add(windowResizedHandler)
	}

	/**
	 * Sets the camera's direction, keeping the up vector orthonormal.
	 */
	override fun setDirection(x: Float, y: Float, z: Float) {
		tmpVec.set(x, y, z)
		if (!tmpVec.isZero()) {
			val dot = tmpVec.dot(up)
			if (MathUtils.isZero(dot - 1)) {
				// Collinear
				up.set(direction).scl(-1f)
			} else if (MathUtils.isZero(dot + 1)) {
				// Collinear opposite
				up.set(direction)
			}
			direction.set(tmpVec)
			normalizeUp()
		}
	}

	/**
	 * Recalculates the direction of the camera to look at the point (x, y, z).
	 */
	override fun pointToLookAt(x: Float, y: Float, z: Float) {
		setDirection(x - position.x, y - position.y, z - position.z)
	}

	/**
	 * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
	 * recalculating the up vector via a cross product between right and direction.
	 */
	protected fun normalizeUp() {
		tmpVec.set(direction).crs(up).nor()
		up.set(tmpVec).crs(direction).nor()
	}

	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and
	 * up vector will not be orthogonalized.
	 *
	 * @param radians the angle in radians
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 */
	fun rotate(radians: Float, axisX: Float, axisY: Float, axisZ: Float) {
		direction.rotateRad(radians, axisX, axisY, axisZ)
		up.rotateRad(radians, axisX, axisY, axisZ)
	}

	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
	 * will not be orthogonalized.
	 *
	 * @param axis the axis to rotate around
	 * @param radians the angle
	 */
	fun rotate(axis: Vector3, radians: Float) {
		direction.rotateRad(axis, radians)
		up.rotateRad(axis, radians)
	}

	/**
	 * Rotates the camera by the given angle around the direction vector. The direction and up vector will not be orthogonalized.
	 * @param radians
	 */
	fun rotate(radians: Float) {
		rotate(direction, radians)
	}

	/**
	 * Rotates the direction and up vector of this camera by the given rotation matrix. The direction and up vector will not be
	 * orthogonalized.
	 *
	 * @param transform The rotation matrix
	 */
	fun rotate(transform: Matrix4) {
		direction.rot(transform)
		up.rot(transform)
	}

	/**
	 * Rotates the direction and up vector of this camera by the given {@link Quaternion}. The direction and up vector will not be
	 * orthogonalized.
	 *
	 * @param quat The quaternion
	 */
	fun rotate(quat: Quaternion) {
		quat.transform(direction)
		quat.transform(up)
	}

	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis, with the axis attached to given
	 * point. The direction and up vector will not be orthogonalized.
	 *
	 * @param point the point to attach the axis to
	 * @param axis the axis to rotate around
	 * @param radians the angle in radians
	 */
	fun rotateAround(point: Vector3, axis: Vector3, radians: Float) {
		tmpVec.set(point)
		tmpVec.sub(position)
		translate(tmpVec)
		rotate(axis, radians)
		tmpVec.rotateRad(axis, radians)
		translate(-tmpVec.x, -tmpVec.y, -tmpVec.z)
	}

	/**
	 * Transform the position, direction and up vector by the given matrix
	 *
	 * @param transform The transform matrix
	 */
	fun transform(transform: Matrix4) {
		position.mul(transform)
		rotate(transform)
	}

	/**
	 * Moves the camera by the given amount on each axis.
	 * @param x the displacement on the x-axis
	 * @param y the displacement on the y-axis
	 * @param z the displacement on the z-axis
	 */
	fun translate(x: Float, y: Float, z: Float = 0f) {
		position.add(x, y, z)
	}

	/**
	 * Moves the camera by the given vector.
	 * @param vec the displacement vector
	 */
	fun translate(vec: Vector3) {
		position.add(vec)
	}

	/**
	 * Moves the camera by the given vector.
	 * @param vec the displacement vector
	 */
	fun translate(vec: Vector2) {
		translate(vec.x, vec.y)
	}

	override fun canvasToGlobal(canvasCoords: Vector3): Vector3 {
		canvasToGlobal(canvasCoords, 0f, 0f, window.width, window.height)
		return canvasCoords
	}

	override fun canvasToGlobal(canvasCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 {
		canvasCoords.x = 2f * (canvasCoords.x - viewportX) / viewportWidth - 1f
		canvasCoords.y = -2f * (canvasCoords.y - viewportY) / viewportHeight + 1f
		canvasCoords.z = 2f * canvasCoords.z - 1f;
		invCombined.prj(canvasCoords)
		return canvasCoords
	}

	override fun project(globalCoords: Vector3): Vector3 {
		project(globalCoords, 0f, 0f, window.width, window.height)
		return globalCoords
	}

	/**
	 * Projects the {@link Vector3} given in global space to screen coordinates. The screen coordinate system has its
	 * origin in the top left, with the y-axis pointing downwards and the x-axis pointing to the right.
	 *
	 * @param viewportX the coordinate of the top left corner of the viewport in glViewport coordinates.
	 * @param viewportY the coordinate of the top left corner of the viewport in glViewport coordinates.
	 * @param viewportWidth the width of the viewport in pixels
	 * @param viewportHeight the height of the viewport in pixels
	 */
	fun project(globalCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 {
		combined.prj(globalCoords)
		globalCoords.x = viewportWidth * (globalCoords.x + 1f) * 0.5f + viewportX
		globalCoords.y = viewportHeight * (-globalCoords.y + 1f) * 0.5f + viewportY
		globalCoords.z = (globalCoords.z + 1f) * 0.5f;
		return globalCoords
	}

	/**
	 * Creates a picking {@link Ray} from the coordinates given in global coordinates. The global coordinates origin
	 * is assumed to be in the top left corner, its y-axis pointing down, the x-axis  pointing to the right.
	 *
	 * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportWidth the width of the viewport in pixels
	 * @param viewportHeight the height of the viewport in pixels
	 * @return The [out] parameter. The Ray will be in global coordinate space.
	 */
	fun getPickRay(canvasX: Float, canvasY: Float, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float, out: Ray): Ray {
		canvasToGlobal(out.origin.set(canvasX, canvasY, -1f), viewportX, viewportY, viewportWidth, viewportHeight)
		canvasToGlobal(out.direction.set(canvasX, canvasY, 0f), viewportX, viewportY, viewportWidth, viewportHeight)
		out.direction.sub(out.origin)
		out.update()
		return out
	}

	override fun getPickRay(canvasX: Float, canvasY: Float, out: Ray): Ray {
		return getPickRay(canvasX, canvasY, 0f, 0f, window.width, window.height, out)
	}

	/**
	 * Moves the position to the point where the camera is looking at the provided coordinates at a given distance.
	 */
	override fun moveToLookAtPoint(x: Float, y: Float, z: Float, distance: Float) {
		tmpVec.set(direction).scl(distance) // Assumes direction is normalized
		position.set(x, y, z).sub(tmpVec)
	}

	/**
	 * Centers the camera on the screen.
	 * [update] must be called after this.
	 */
	override fun centerCamera() {
		val width = maxOf(1f, window.width)
		val height = maxOf(1f, window.height)
		setViewport(width, height)
		moveToLookAtRect(0f, 0f, width, height)
	}

	override fun dispose() {
		autoCenter = false
	}

}
