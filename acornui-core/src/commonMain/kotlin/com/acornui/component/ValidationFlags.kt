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

import kotlin.math.log2

/**
 * A list of validation bit flags Acorn internally uses.
 * Extended validation flags should start at `1 shl 16`
 *
 * @author nbilyk
 */
object ValidationFlags {

	const val STYLES: Int = 1 shl 0

	const val PROPERTIES: Int = 1 shl 1

	/**
	 * A descendant has added, removed, or reordered a child.
	 */
	const val HIERARCHY_ASCENDING: Int = 1 shl 2

	/**
	 * An ancestor has changed.
	 */
	const val HIERARCHY_DESCENDING: Int = 1 shl 3

	const val SIZE_CONSTRAINTS: Int = 1 shl 4

	const val LAYOUT: Int = 1 shl 5
	const val LAYOUT_ENABLED: Int = 1 shl 6

	const val TRANSFORM: Int = 1 shl 7

	const val INTERACTIVITY_MODE: Int = 1 shl 8

	const val RENDER_CONTEXT: Int = 1 shl 9
	const val BITMAP_CACHE: Int = 1 shl 10
	const val REDRAW_REGION: Int = 1 shl 11

	const val RESERVED_1: Int = 1 shl 12
	const val RESERVED_2: Int = 1 shl 13
	const val RESERVED_3: Int = 1 shl 14
	const val RESERVED_4: Int = 1 shl 15

	/**
	 * Prints out the name of the flag for reserved flags, or the power of two for non-reserved flags.
	 */
	fun flagToString(flag: Int): String = when (flag) {
		STYLES -> "STYLES"
		PROPERTIES -> "PROPERTIES"
		HIERARCHY_ASCENDING -> "HIERARCHY_ASCENDING"
		HIERARCHY_DESCENDING -> "HIERARCHY_DESCENDING"
		SIZE_CONSTRAINTS -> "SIZE_CONSTRAINTS"
		LAYOUT -> "LAYOUT"
		LAYOUT_ENABLED -> "LAYOUT_ENABLED"
		TRANSFORM -> "TRANSFORM"

		INTERACTIVITY_MODE -> "INTERACTIVITY_MODE"

		RENDER_CONTEXT-> "RENDER_CONTEXT"
		BITMAP_CACHE -> "BITMAP_CACHE"

		RESERVED_1 -> "RESERVED_1"
		RESERVED_2 -> "RESERVED_2"
		RESERVED_3 -> "RESERVED_3"
		RESERVED_4 -> "RESERVED_4"
		else -> log2(flag.toDouble()).toInt().toString()
	}

	/**
	 * Using the ValidationFlags list, prints out a comma separated list of the flags this bit mask contains.
	 * For non-reserved flags (flags at least 1 shl 16), the power of two will be printed.
	 * @see ValidationFlags
	 * @see ValidationFlags.flagToString
	 */
	fun flagsToString(flags: Int): String {
		var str = ""
		for (i in 0..31) {
			val flag = 1 shl i
			if (flag and flags > 0) {
				if (str.isNotEmpty()) str += ","
				str += flagToString(flag)
			}
		}
		return str
	}

}

fun Validatable.invalidateSize() {
	invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	Unit
}

fun Validatable.invalidateLayout() {
	invalidate(ValidationFlags.LAYOUT)
	Unit
}

fun Validatable.invalidateProperties() {
	invalidate(ValidationFlags.PROPERTIES)
	Unit
}
