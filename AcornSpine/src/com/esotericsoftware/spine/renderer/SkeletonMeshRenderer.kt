/*
 * Spine Runtimes Software License
 * Version 2.3
 *
 * Copyright (c) 2013-2015, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to use, install, execute and perform the Spine
 * Runtimes Software (the "Software") and derivative works solely for personal
 * or internal use. Without the written permission of Esoteric Software (see
 * Section 2 of the Spine Software License Agreement), you may not (a) modify,
 * translate, adapt or otherwise create derivative works, improvements of the
 * Software or develop new applications using the Software or (b) remove,
 * delete, alter or obscure any trademarks or any copyright, trademark, patent
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.esotericsoftware.spine.renderer

import com.acornui.core.graphics.Texture
import com.acornui.gl.core.GlState
import com.acornui.gl.core.Vertex
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.Vector3
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment
import com.esotericsoftware.spine.attachments.SkeletonAttachment
import com.esotericsoftware.spine.attachments.WeightedMeshAttachment
import com.esotericsoftware.spine.component.LoadedSkeleton

object SkeletonMeshRenderer : SkeletonRenderer {

	private val quadTriangles = shortArrayOf(0, 1, 2, 2, 3, 0)

	override fun draw(loadedSkeleton: LoadedSkeleton, skeleton: Skeleton, glState: GlState, concatenatedTransform: Matrix4Ro, concatenatedColorTint: ColorRo) {
		val skin = skeleton.currentSkin ?: skeleton.defaultSkin ?: return // No skin to render.
		val loadedSkin = loadedSkeleton.loadedSkins[skin.data] ?: return // Skin not loaded.

		var vertices: Array<Vertex>? = null
		var triangles: ShortArray? = null
		val drawOrder = skeleton.drawOrder
		var i = 0
		val n = drawOrder.size
		while (i < n) {
			val slot = drawOrder[i]
			val attachment = slot.attachment
			var texture: Texture? = null
			if (attachment is RegionAttachment) {
				vertices = attachment.updateWorldVertices(slot)
				triangles = quadTriangles
				texture = loadedSkin.getTexture(attachment.page)

			} else if (attachment is MeshAttachment) {
				vertices = attachment.updateWorldVertices(slot)
				triangles = attachment.data.triangles
				texture = loadedSkin.getTexture(attachment.page)

			} else if (attachment is WeightedMeshAttachment) {
				vertices = attachment.updateWorldVertices(slot)
				triangles = attachment.data.triangles
				texture = loadedSkin.getTexture(attachment.page)

			} else if (attachment is SkeletonAttachment) {
				val attachmentSkeleton = attachment.skeleton
				if (attachmentSkeleton == null) {
					i++
					continue
				}
				val bone = slot.bone
				val rootBone = attachmentSkeleton.rootBone!!
				val oldScaleX = rootBone.scaleX
				val oldScaleY = rootBone.scaleY
				val oldRotation = rootBone.rotation
				attachmentSkeleton.setPosition(skeleton.x + bone.worldX, skeleton.y + bone.worldY)
				rootBone.scaleX = (1f + bone.worldScaleX - oldScaleX)
				rootBone.scaleY = (1f + bone.worldScaleY - oldScaleY)
				rootBone.rotation = oldRotation + bone.worldRotationX
				attachmentSkeleton.updateWorldTransform()

				SkeletonMeshRenderer.draw(loadedSkeleton, attachmentSkeleton, glState, concatenatedTransform, concatenatedColorTint)

				attachmentSkeleton.setPosition(0f, 0f)
				rootBone.scaleX = oldScaleX
				rootBone.scaleY = oldScaleY
				rootBone.rotation = oldRotation
			}
			if (texture != null) {
				val batch = glState.batch
				glState.blendMode(slot.data.blendMode, premultipliedAlpha = false)
				glState.setTexture(texture)
				batch.begin()
				val v = vertices!!
				for (j in 0..v.lastIndex) {
					val vertex = v[j]
					concatenatedTransform.prj(vertex.position)
					concatenatedTransform.rot(vertex.normal.set(Vector3.NEG_Z))
					vertex.colorTint.mul(concatenatedColorTint)
					batch.putVertex(vertex)
				}
				val t = triangles!!
				val highestIndex = batch.highestIndex + 1
				for (j in 0..t.lastIndex) {
					batch.putIndex(t[j] + highestIndex)
				}
			}
			i++
		}
	}

}
