package com.acornui.core.graphics

import com.acornui.async.then
import com.acornui.collection.fill
import com.acornui.component.ComponentInit
import com.acornui.component.ContainerImpl
import com.acornui.component.ValidationFlags
import com.acornui.core.AppConfig
import com.acornui.core.assets.CachedGroup
import com.acornui.core.assets.cachedGroup
import com.acornui.core.assets.loadAndCacheJson
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.io.JSON_KEY
import com.acornui.core.time.onTick
import com.acornui.math.Bounds


class SpriteAnimation(owner: Owned) : ContainerImpl(owner) {

	val json = inject(JSON_KEY)

	/**
	 * The current animation frame. This will increment after this animation's framerate duration has passed.
	 */
	var currentFrame = 0

	private var _startFrame = 0

	/**
	 * After [endFrame] the animation will start back on this index.
	 */
	val startFrame: Int
		get() = _startFrame

	private var _endFrame = -1

	/**
	 * After this frame, the animation will loop back to [startFrame]
	 * If this value is negative, the last frame in the sequence will be used.
	 */
	val endFrame: Int
		get() = _endFrame

	var frameRate: Int = inject(AppConfig).frameRate

	val stepTime: Float
		get() = 1f / frameRate.toFloat()

	/**
	 * If true, this animation will not progress.
	 */
	var paused: Boolean = false

	private var elapsed: Float = 0f

	private var regionWidth: Int = 0
	private var regionHeight: Int = 0

	private val frameClips = ArrayList<AtlasComponent>()

	init {
		onTick {
			if (!paused && frameClips.isNotEmpty()) {
				val stepTime = this.stepTime

				elapsed += it
				while (elapsed >= stepTime) {
					// Tick a frame
					elapsed -= stepTime
					frameClips[currentFrame - startFrame].visible = false // Hide the old frame clip.
					if (++currentFrame > _endFrame) {
						currentFrame = _startFrame
					}
					frameClips[currentFrame - startFrame].visible = true // Show the current frame clip.
				}
			}
		}
	}

	private var group: CachedGroup? = null

	fun setRegion(atlasPath: String, regionName: String, startFrame: Int = 0, endFrame: Int = -1) {
		for (i in 0..frameClips.lastIndex) {
			removeChild(frameClips[i])
			frameClips[i].dispose()
		}
		frameClips.clear()

		group?.dispose()
		group = cachedGroup()
		loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group!!).then {
			atlasData ->
			val regions = ArrayList<AtlasRegionData?>()
			regionWidth = 0
			for (page in atlasData.pages) {
				for (region in page.regions) {
					val index = region.name.calculateFrameIndex(regionName)
					if (index >= startFrame && (endFrame == -1 || index <= endFrame)) {
						regions.fill(index - startFrame + 1) { null }
						regions[index - startFrame] = region
						if (region.originalWidth > regionWidth) {
							regionWidth = region.originalWidth
						}
						if (region.originalHeight > regionHeight) {
							regionHeight = region.originalHeight
						}
					}
				}
			}

			for (i in 0..regions.lastIndex) {
				val region = regions[i] ?: continue

				val atlasComponent = addChild(atlas(atlasPath, region.name))
				frameClips.add(atlasComponent)
			}
			_startFrame = startFrame
			_endFrame = frameClips.lastIndex + _startFrame

			invalidate(ValidationFlags.LAYOUT)
		}
	}

	/**
	 * If the region name matches, returns the index of the frame. Otherwise, returns -1
	 */
	private fun String.calculateFrameIndex(name: String): Int {
		if (this.startsWith(name)) {
			if (this.length == name.length) return 0
			if (this[name.length] == '.') return 0
			if (this[name.length] == '_') {
				var lastIndex = lastIndexOf('.')
				if (lastIndex == -1)
					lastIndex = length
				return substring(name.length + 1, lastIndex).toIntOrNull() ?: -1
			}
		}
		return -1
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.set(regionWidth.toFloat(), regionHeight.toFloat())
	}

	override fun draw(viewportX: Float, viewportY: Float, viewportRight: Float, viewportBottom: Float) {
		if (currentFrame >= startFrame && (currentFrame - startFrame) < frameClips.size) {
			val frameClip = frameClips[currentFrame - startFrame]
			if (frameClip.visible)
				frameClip.render(viewportX, viewportY, viewportRight, viewportBottom)
		}
	}

	override fun dispose() {
		super.dispose()
		group?.dispose()
		group = null
	}
}

fun Owned.spriteAnimation(atlasPath: String, regionName: String, startFrame: Int = 0, endFrame: Int = -1, init: ComponentInit<SpriteAnimation> = {}): SpriteAnimation {
	val s = SpriteAnimation(this)
	s.setRegion(atlasPath, regionName, startFrame, endFrame)
	s.init()
	return s
}

fun Owned.spriteAnimation(init: ComponentInit<SpriteAnimation> = {}): SpriteAnimation {
	val s = SpriteAnimation(this)
	s.init()
	return s
}