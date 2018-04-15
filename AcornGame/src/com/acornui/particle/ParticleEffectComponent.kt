/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.particle

import com.acornui.async.launch
import com.acornui.component.InteractivityMode
import com.acornui.component.UiComponentImpl
import com.acornui.core.*
import com.acornui.core.assets.*
import com.acornui.core.di.Owned
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.TextureAtlasDataSerializer
import com.acornui.core.graphics.loadAndCacheAtlasPage
import com.acornui.core.time.onTick
import com.acornui.gl.component.Sprite
import com.acornui.gl.core.GlState
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4Ro

class ParticleEffectComponent(
		owner: Owned
) : UiComponentImpl(owner) {

	private val glState = inject(GlState)

	init {
		interactivityMode = InteractivityMode.NONE

		onTick {
			val effect = _effect
			if (effect != null) {
				effect.update(it)
				window.requestRender()
			}
		}
	}

	/**
	 * Loads a particle effect and its textures, then assigning it to this component.
	 * @param pDataPath The path to the particle effect json.
	 * @param atlasPath The path to the atlas json for where the texture atlas the particle images are located.
	 * @param disposeOld If true, the old effect will be disposed and cached files decremented.
	 */
	fun load(pDataPath: String, atlasPath: String, disposeOld: Boolean = true) {
		launch {
			val oldEffect = _effect
			effect = loadParticleEffect(pDataPath, atlasPath)
			if (disposeOld)
				oldEffect?.dispose() // Dispose after load in order to reuse cached files.
		}
	}

	override fun onActivated() {
		super.onActivated()
		_effect?.activate()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		_effect?.deactivate()
	}

	private var _effect: LoadedParticleEffect? = null

	private var effect: LoadedParticleEffect?
		get() = _effect
		set(value) {
			val oldValue = _effect
			if (oldValue == value) return
			if (oldValue != null && isActive) {
				oldValue.deactivate()
			}
			_effect = value
			if (value != null && isActive) {
				value.activate()
			}
		}

	fun effect(value: LoadedParticleEffect?) {
		effect = value
	}

	override fun draw() {
		val effect = _effect ?: return
		glState.camera(camera)
		effect.render(concatenatedTransform, concatenatedColorTint)
	}

	override fun dispose() {
		effect = null
		super.dispose()
	}
}

class LoadedParticleEffect(

		effectInstance: ParticleEffectInstanceVo,

		private val renderers: List<ParticleEmitterRenderer>,

		/**
		 * The cached group the particle effect used to load all the files.
		 */
		private val group: CachedGroup
) : LifecycleBase(), Updatable {

	private val emitterInstances = effectInstance.emitterInstances

	init {
	}

	override fun onActivated() {
		super.onActivated()
		for (i in 0..renderers.lastIndex) {
			renderers[i].activate()
		}
	}

	override fun onDeactivated() {
		super.onDeactivated()
		for (i in 0..renderers.lastIndex) {
			renderers[i].deactivate()
		}
	}

	override fun update(stepTime: Float) {
		for (i in 0..emitterInstances.lastIndex) {
			emitterInstances[i].update(stepTime)
		}
	}

	override fun dispose() {
		group.dispose()
	}

	fun render(concatenatedTransform: Matrix4Ro, concatenatedColorTint: ColorRo) {
		for (i in 0..renderers.lastIndex) {
			renderers[i].render(concatenatedTransform, concatenatedColorTint)
		}
	}
}

suspend fun Scoped.loadParticleEffect(pDataPath: String, atlasPath: String): LoadedParticleEffect {
	val group = cachedGroup()
	val atlasDataPromise = loadAndCacheJson(atlasPath, TextureAtlasDataSerializer, group)
	val effectData = loadAndCacheJson(pDataPath, ParticleEffectSerializer, group).await()
	val atlasData = atlasDataPromise.await()
	val emitterRenderers = ArrayList<ParticleEmitterRenderer>(effectData.emitters.size)
	val effectInstance = effectData.createInstance()

	for (emitterInstance in effectInstance.emitterInstances) {
		val emitter = emitterInstance.emitter
		val sprites = ArrayList<Sprite>(emitter.imageEntries.size)
		for (i in 0..emitter.imageEntries.lastIndex) {
			val imagePath = emitter.imageEntries[i].path

			val (page, region) = atlasData.findRegion(imagePath) ?: throw Exception("Could not find $imagePath in the atlas $atlasPath")
			val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()

			val sprite = Sprite()
			sprite.texture = texture
			sprite.isRotated = region.isRotated
			sprite.setRegion(region.bounds)
			sprite.updateUv()
			sprites.add(sprite)
		}
		emitterRenderers.add(ParticleEmitterRenderer2d(injector, emitterInstance, sprites))
	}
	return LoadedParticleEffect(effectInstance, emitterRenderers, group)
}

interface ParticleEmitterRenderer : Lifecycle {

	fun render(concatenatedTransform: Matrix4Ro, concatenatedColorTint: ColorRo)
}


fun Owned.particleEffectComponent(init: ParticleEffectComponent.() -> Unit = {}): ParticleEffectComponent {
	val p = ParticleEffectComponent(this)
	p.init()
	return p
}

fun Owned.particleEffectComponent(pDataPath: String, atlasPath: String, init: ParticleEffectComponent.() -> Unit = {}): ParticleEffectComponent {
	val p = ParticleEffectComponent(this)
	p.load(pDataPath, atlasPath)
	p.init()
	return p
}
