package com.acornui.nav

import com.acornui.di.ContextImpl
import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

class NavigationManagerTest {

	@Test fun navNode() {
		var n1 = NavNode("Hi", hashMapOf(Pair("test1", "test2")))
		var n2 = NavNode("Hi", hashMapOf(Pair("test1", "test3")))

		assertNotEquals(n1, n2)

		n1 = NavNode("Hi", hashMapOf(Pair("test1", "test2")))
		n2 = NavNode("Hi", hashMapOf(Pair("test1", "test2")))

		assertEquals(n1, n2)
	}

	@Test fun push() {
		val m = NavigationManagerImpl(ContextImpl())
		m.push(NavNode("Hi", hashMapOf(Pair("test1", "test2"))))

		assertListEquals(listOf(NavNode("Hi", hashMapOf(Pair("test1", "test2")))), m.path())

		m.push(NavNode("Bye", hashMapOf(Pair("test2", "test4"))))

		assertListEquals(listOf(
				NavNode("Hi", hashMapOf(Pair("test1", "test2"))),
				NavNode("Bye", hashMapOf(Pair("test2", "test4")))
		), m.path())
	}

	@Test fun pop() {
		val m = NavigationManagerImpl(ContextImpl())
		m.push(NavNode("Hi", hashMapOf(Pair("test1", "test2"))))
		m.push(NavNode("Bye", hashMapOf(Pair("test2", "test4"))))
		m.pop()
		assertListEquals(listOf(NavNode("Hi", hashMapOf(Pair("test1", "test2")))), m.path())
		m.pop()
		assertListEquals(listOf(), m.path())
		m.pop()
		assertListEquals(listOf(), m.path())
	}

	@Test fun changed() {
		val m = NavigationManagerImpl(ContextImpl())
		m.changed.add({
			event ->
			assertListEquals(listOf(), event.oldPath)
			assertListEquals(listOf(NavNode("Hi", hashMapOf(Pair("test1", "test2")))), event.newPath)
		}, isOnce = true)
		m.push(NavNode("Hi", hashMapOf(Pair("test1", "test2"))))

		m.changed.add({
			event ->
			assertListEquals(listOf(NavNode("Hi", hashMapOf(Pair("test1", "test2")))), event.oldPath)
			assertListEquals(listOf(
					NavNode("Hi", hashMapOf(Pair("test1", "test2"))),
					NavNode("Bye", hashMapOf(Pair("test2", "test4")))
			), event.newPath)
		}, isOnce = true)
		m.push(NavNode("Bye", hashMapOf(Pair("test2", "test4"))))

		m.changed.add({
			event ->
			assertListEquals(listOf(
					NavNode("Hi", hashMapOf(Pair("test1", "test2"))),
					NavNode("Bye", hashMapOf(Pair("test2", "test4")))
			), event.oldPath)
			assertListEquals(listOf(
					NavNode("Hi", hashMapOf(Pair("test1", "test2")))
			), event.newPath)
		}, isOnce = true)
		m.pop()
		m.changed.add({
			event ->
			assertListEquals(listOf(
					NavNode("Hi", hashMapOf(Pair("test1", "test2")))
			), event.oldPath)
			assertListEquals(listOf(), event.newPath)
		}, isOnce = true)
		m.pop()
		val shouldNotChange = {
			_: NavEvent ->
			fail("Should not have changed")
		}
		m.changed.add(shouldNotChange)
		m.pop()
		m.changed.remove(shouldNotChange)

		m.changed.add({
			event ->
			assertListEquals(listOf(), event.oldPath)
			assertListEquals(listOf(
					NavNode("Hi", hashMapOf(Pair("test1", "test2"))),
					NavNode("Bye", hashMapOf(Pair("test2", "test4")))
			), event.newPath)
		}, isOnce = true)
		m.path(listOf(
				NavNode("Hi", hashMapOf(Pair("test1", "test2"))),
				NavNode("Bye", hashMapOf(Pair("test2", "test4")))
		))

		m.changed.add(shouldNotChange)
		m.path(listOf(
				NavNode("Hi", hashMapOf(Pair("test1", "test2"))),
				NavNode("Bye", hashMapOf(Pair("test2", "test4")))
		))
		m.changed.remove(shouldNotChange)
	}

	/**
	 * Test that the path can be set in a path changed handler.
	 */
	@Test fun concurrentPath() {
		val m = NavigationManagerImpl(ContextImpl())
		m.changed.add({
			_ ->
			m.path(listOf(NavNode("test2")))
		}, isOnce = true)

		m.changed.add({
			event ->
			assertEquals("test1", event.newPath.first().name)
			assertEquals("test2", m.path().first().name)
		}, isOnce = true)
		m.path(listOf(NavNode("test1")))

		assertEquals("test2", m.path().first().name)
	}
}