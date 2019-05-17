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

package com.acornui.gl.core

import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.RgbData
import com.acornui.io.byteBuffer

class RgbTexture(
		gl: Gl20,
		glState: GlState,
		override val rgbData: RgbData
) : GlTextureBase(gl, glState) {

	init {
		pixelFormat = if (rgbData.hasAlpha) TexturePixelFormat.RGBA else TexturePixelFormat.RGB
	}

	override val width: Int
		get() = rgbData.width

	override val height: Int
		get() = rgbData.height
	
	override fun uploadTexture() {
		val buffer = byteBuffer(rgbData.bytes.size)
		for (i in 0..rgbData.bytes.lastIndex) {
			buffer.put(rgbData.bytes[i])
		}
		buffer.flip()
		gl.texImage2Db(target.value, 0, pixelFormat.value, rgbData.width, rgbData.height, 0, pixelFormat.value, pixelType.value, buffer)
	}
}


fun rgbTexture(gl: Gl20, glState: GlState, rgbData: RgbData, init: RgbTexture.() -> Unit = {}): RgbTexture {
	val r = RgbTexture(gl, glState, rgbData)
	r.init()
	return r
}

fun Scoped.rgbTexture(rgbData: RgbData, init: RgbTexture.() -> Unit = {}): RgbTexture {
	val r = RgbTexture(inject(Gl20), inject(GlState), rgbData)
	r.init()
	return r
}
