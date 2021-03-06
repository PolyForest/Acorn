/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.collection

import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotSame

class ListUtilsKtTest {
	@Test
	fun copyIntoArrayList() {
		run {
			val f = mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0)
			f.copyInto(f, destinationOffset = 0, startIndex = 1, endIndex = 5)
            assertListEquals(listOf(2.0, 3.0, 4.0, 5.0, 5.0), f)
		}

		run {
			val f = mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0)
			f.copyInto(f, destinationOffset = 1, startIndex = 0, endIndex = 4)
            assertListEquals(listOf(1.0, 1.0, 2.0, 3.0, 4.0), f)
		}

		run {
			val f = mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0)
			f.copyInto(f, destinationOffset = 3)
            assertListEquals(listOf(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 4.0, 5.0), f)
		}

		run {
			val f = mutableListOf(0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0)
			f.copyInto(f, destinationOffset = 1, startIndex = 4, endIndex = 7)
            assertListEquals(listOf(0.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 4.0, 5.0), f)
		}

		run {
			val src = mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0)
			val dest = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0)
			src.copyInto(dest, destinationOffset = 1)
            assertListEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), dest)
		}

		run {
			val src = mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0)
			val dest = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0)
			src.copyInto(dest, destinationOffset = 1, startIndex = 3)
            assertListEquals(listOf(0.0, 4.0, 5.0, 0.0, 0.0), dest)
		}

		run {
			val src = mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0)
			val dest = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0)
			src.copyInto(dest, destinationOffset = 3, startIndex = 1)
            assertListEquals(listOf(0.0, 0.0, 0.0, 2.0, 3.0, 4.0, 5.0), dest)
		}
	}

	@Test
	fun addAllUnique() {
	}

	@Test
	fun addAllUnique1() {
	}

	@Test
	fun addSorted() {
	}

	@Test
	fun addSorted1() {
	}

	@Test
	fun addOrSet() {
	}

	@Test
	fun fill() {
	}

	@Test
	fun addAll2() {
	}

	@Test
	fun addAll21() {
	}

	@Test
	fun toList() {
	}

	@Test
	fun filterTo2() {
	}

	@Test
	fun find2() {
	}

	@Test
	fun indexOfFirst() {
		val list = arrayListOf(0, 1, 2, 3, 4, 5, 0)
		assertEquals(0, list.indexOfFirst { it == 0 })
		assertEquals(-1, list.indexOfFirst(1, 2) { it == 0 })
		assertEquals(6, list.indexOfFirst(1, 6) { it == 0 })
	}

	@Test
	fun indexOfLast2() {
		val list = arrayListOf(0, 1, 2, 3, 4, 5, 0)
		assertEquals(6, list.indexOfLast { it == 0 })
		assertEquals(-1, list.indexOfLast(2, 1) { it == 0 })
		assertEquals(6, list.indexOfLast(6, 1) { it == 0 })
	}

	@Test
	fun emptyList() {
		val list = emptyList<Int>()
		assertEquals(-1, list.indexOfLast { it == 0 })
		assertEquals(-1, list.indexOfFirst { it == 0 })
		assertEquals(-1, list.indexOfLast(0, 1) { it == 0 })
		assertEquals(-1, list.indexOfFirst(0, 1) { it == 0 })
		assertEquals(-1, list.indexOfLast(1, 1) { it == 0 })
		assertEquals(-1, list.indexOfFirst(1, 1) { it == 0 })
	}

	@Test
	fun setSize() {
		val list = arrayListOf(1, 2, 3)
		list.setSize(5) { 0 }
        assertListEquals(listOf(1, 2, 3, 0, 0), list)

		list.setSize(3) { 0 }
        assertListEquals(listOf(1, 2, 3), list)
	}

	@Test
	fun replaceRange() {
		val list = arrayListOf(0, 1, 2, 3)
		assertEquals(listOf(0, 1, 5, 6), list.replaceRange(2, 4, listOf(5, 6)))
		assertEquals(listOf(0, 1, 5, 6, 2, 3), list.replaceRange(2, 2, listOf(5, 6)))

	}

	@Test
	fun sortedInsertionIndex() {
		val arr = arrayListOf(1, 4, 6, 7, 8)
		assertEquals(2, arr.sortedInsertionIndex(5))
		assertEquals(3, arr.sortedInsertionIndex(6))
		assertEquals(4, arr.sortedInsertionIndex(7))
		assertEquals(5, arr.sortedInsertionIndex(8))
		assertEquals(5, arr.sortedInsertionIndex(9))
	}

	@Test
	fun sortedInsertionIndexWithComparator() {
		val comparator = { o1: Int?, o2: Int? ->
			if (o1 == null && o2 == null) 0
			else if (o1 == null) -1
			else if (o2 == null) 1
			else o1.compareTo(o2)
		}

		val arr = arrayListOf(1, 4, 6, 7, 8)
		assertEquals(0, arr.sortedInsertionIndex(0, comparator = comparator))
		assertEquals(5, arr.sortedInsertionIndex(9, comparator = comparator))
		assertEquals(1, arr.sortedInsertionIndex(1, comparator = comparator))
		assertEquals(2, arr.sortedInsertionIndex(5, comparator = comparator))
		assertEquals(3, arr.sortedInsertionIndex(6, comparator = comparator))
		assertEquals(4, arr.sortedInsertionIndex(7, comparator = comparator))
		assertEquals(5, arr.sortedInsertionIndex(8, comparator = comparator))
		assertEquals(5, arr.sortedInsertionIndex(9, comparator = comparator))

		assertEquals(2, arr.sortedInsertionIndex(5, fromIndex = 2, toIndex = 4, comparator = comparator))
		assertEquals(2, arr.sortedInsertionIndex(0, fromIndex = 2, toIndex = 4, comparator = comparator))
		assertEquals(4, arr.sortedInsertionIndex(7, fromIndex = 2, toIndex = 5, comparator = comparator))
		assertEquals(4, arr.sortedInsertionIndex(7, fromIndex = 2, toIndex = 4, comparator = comparator))
		assertEquals(4, arr.sortedInsertionIndex(10, fromIndex = 2, toIndex = 4, comparator = comparator))
	}

	@Test
	fun orderedColumns() {
		val arr = arrayListOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 4, 5)
		assertEquals(4, arr.sortedInsertionIndex(4, fromIndex = 0, toIndex = 4))
		assertEquals(1, arr.sortedInsertionIndex(0, fromIndex = 0, toIndex = 4))
		assertEquals(5, arr.sortedInsertionIndex(0, fromIndex = 4, toIndex = 8))
		assertEquals(6, arr.sortedInsertionIndex(1, fromIndex = 4, toIndex = 8))
		assertEquals(5, arr.sortedInsertionIndex(1, fromIndex = 4, toIndex = 8, matchForwards = false))
		assertEquals(6, arr.sortedInsertionIndex(2, fromIndex = 4, toIndex = 8, matchForwards = false))
		assertEquals(7, arr.sortedInsertionIndex(2, fromIndex = 4, toIndex = 8))
		assertEquals(15, arr.sortedInsertionIndex(2, fromIndex = 12, toIndex = 16))
		assertEquals(14, arr.sortedInsertionIndex(1, fromIndex = 12, toIndex = 16))
		assertEquals(16, arr.sortedInsertionIndex(3, fromIndex = 12))
		assertEquals(12, arr.sortedInsertionIndex(-1, fromIndex = 12))
		assertEquals(13, arr.sortedInsertionIndex(0, fromIndex = 12, matchForwards = true))

	}

	@Test
	fun shiftAll() {
		val list = ArrayList<Int>(16)
		list.addAll(0, 1, 2, 3, 4, 5, 6)
		list.shiftAll(3)
        assertListEquals(listOf(3, 4, 5, 6, 0, 1, 2), list)

		list.shiftAll(-3)
        assertListEquals(listOf(0, 1, 2, 3, 4, 5, 6), list)
	}

	@Test
	fun subListSafe() {
		val list = listOf(1, 2, 3, 4)
        assertListEquals(listOf(2, 3), list.subListSafe(1, 3))
        assertListEquals(listOf(2, 3, 4), list.subListSafe(1, 4))
        assertListEquals(listOf(2, 3, 4), list.subListSafe(1, 5))
        assertListEquals(listOf(1, 2, 3, 4), list.subListSafe(0, 5))
        assertListEquals(listOf(1, 2, 3, 4), list.subListSafe(-1, 5))
	}

	@Test
	fun addBefore() {
		val list = mutableListOf(1, 3, 5)
		list.addBefore(4, 5)
		list.addBefore(2, 3)
        assertListEquals(listOf(1, 2, 3, 4, 5), list)

		assertFails {
			list.addBefore(10, -1)
		}
	}

	@Test
	fun addAfter() {
		val list = mutableListOf(1, 3, 5)
		list.addAfter(4, 3)
		list.addAfter(2, 1)
        assertListEquals(listOf(1, 2, 3, 4, 5), list)

		assertFails {
			list.addBefore(10, -1)
		}
	}


	@Test
	fun copy() {
		val list = listOf(1, 2, 3, 4)
        assertListEquals(list, list.copy())
		assertNotSame(list, list.copy())
        assertListEquals(emptyList<Int>(), emptyList<Int>().copy())
	}

	@Test
	fun removeIndices() {
		val list = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
		assertListEquals(listOf(0, 2, 4, 6, 7, 8), list.removeIndices(listOf(1, 3, 5)))
		assertListEquals(listOf(0, 2, 4, 6, 7, 8), list.removeIndices(listOf(5, 3, 1)))
		assertListEquals(listOf(0, 2, 4, 6, 8), list.removeIndices(listOf(5, 1, 3, 7)))
	}

}