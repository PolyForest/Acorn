/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.core.graphics.RgbData
import com.acornui.core.io.BufferFactory

class RgbTexture(
		private val gl: Gl20,
		private val glState: GlState,
		private val rgbData: RgbData
) : GlTextureBase(gl, glState) {

	override fun width(): Int {
		return rgbData.width
	}

	override fun height(): Int {
		return rgbData.height
	}

	override fun rgbData(): RgbData {
		return rgbData
	}

	override fun uploadTexture() {
		val buffer = BufferFactory.instance.byteBuffer(rgbData.bytes.size)
		for (i in 0..rgbData.bytes.lastIndex) {
			buffer.put(rgbData.bytes[i])
		}
		buffer.flip()
		gl.texImage2Db(target.value, 0, pixelFormat.value, rgbData.width, rgbData.height, 0, pixelFormat.value, pixelType.value, buffer)
	}
}


fun rgbTexture(gl: Gl20, glState: GlState, rgbData: RgbData, init: RgbTexture.()->Unit): RgbTexture {
	val r = RgbTexture(gl, glState, rgbData)
	r.init()
	return r
}