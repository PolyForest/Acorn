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

@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate", "unused")

package com.acornui.component

import com.acornui.Disposable
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.HorizontalLayoutContainer
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.layout.algorithm.hGroup
import com.acornui.component.layout.algorithm.scaleGroup
import com.acornui.component.scroll.scrollArea
import com.acornui.component.style.*
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.factory.LazyInstance
import com.acornui.factory.lazyInstance
import com.acornui.graphic.Scaling
import com.acornui.i18n.i18nBundleName
import com.acornui.i18n.labelI18n
import com.acornui.input.interaction.ClickInteractionRo
import com.acornui.input.interaction.click
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.signal.Cancel
import com.acornui.signal.Signal3
import com.acornui.signal.Signal4
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TabNavigator(owner: Context) : ContainerImpl(owner), LayoutDataProvider<StackLayoutData> {

	private val _userCurrentIndexChanged = own(Signal4<TabNavigator, Int, Int, Cancel>())

	/**
	 * Dispatched when the current tab index is about to change due to a tab click event.
	 * The handler should have the signature:
	 * (this: TabNavigator, previousIndex: Int, newIndex: Int, cancel: Cancel)
	 */
	val userCurrentIndexChanged = _userCurrentIndexChanged.asRo()

	private val _currentIndexChanged = own(Signal3<TabNavigator, Int, Int>())

	/**
	 * Dispatched when the current tab index has changed. The handler should have the signature
	 * (this: TabNavigator, previousIndex: Int, newIndex: Int)
	 */
	val currentIndexChanged = _currentIndexChanged.asRo()

	val style = bind(TabNavigatorStyle())

	/**
	 * The scroll area in which the tab components will be placed.
	 */
	protected val contents = scrollArea()

	protected val _tabBarContainer: UiComponent

	/**
	 * The container for the tab buttons.
	 */
	val tabBarContainer: UiComponentRo
		get() = _tabBarContainer

	private val tabBar: HorizontalLayoutContainer<Button>

	private var background: UiComponent? = null
	private val _tabs = ArrayList<TabNavigatorTab>()

	/**
	 * Returns the list of tabs.
	 */
	val tabs: List<TabNavigatorTab>
		get() = _tabs

	private var _currentIndex = 0

	private var selectedTab: TabNavigatorTab? = null

	private val cancel = Cancel()

	/**
	 * The measured height of the tab bar (including tab padding).
	 */
	var tabBarHeight: Float = 0f
		get() {
			validate(ValidationFlags.LAYOUT)
			return field
		}
		protected set

	/**
	 * The measured width of the tab bar (including tab padding).
	 */
	var tabBarWidth: Float = 0f
		get() {
			validate(ValidationFlags.LAYOUT)
			return field
		}
		protected set

	init {
		styleTags.add(TabNavigator)
		_tabBarContainer = scaleGroup {
			interactivityMode = InteractivityMode.CHILDREN
			style.scaling = Scaling.STRETCH_X
			style.horizontalAlign = HAlign.LEFT
			style.verticalAlign = VAlign.BOTTOM
			tabBar = +hGroup<Button> {
				interactivityMode = InteractivityMode.CHILDREN
				style.verticalAlign = VAlign.BOTTOM
			} layout {
				widthPercent = 1f
				maxScaleX = 1f
				maxScaleY = 1f
			}
		}
		addChild(_tabBarContainer)
		addChild(contents)

		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))
			tabBar.style.gap = it.tabGap
			tabBar.style.horizontalAlign = it.tabsHorizontalAlign
		}
	}

	private fun tabClickHandler(e: ClickInteractionRo) {
		if (!e.handled) {
			val index = tabBar.elements.indexOf(e.currentTarget)
			if (_currentIndex != index) {
				e.handled = true
				_userCurrentIndexChanged.dispatch(this, _currentIndex, index, cancel.reset())
				if (!cancel.isCancelled) {
					currentIndex = index
				}
			}
		}
	}

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	var currentIndex: Int
		get() = _currentIndex
		set(value) {
			val previousIndex = _currentIndex
			if (value == previousIndex) return
			_currentIndex = value
			updateSelectedTab()
			_currentIndexChanged.dispatch(this, previousIndex, value)
		}

	/**
	 * Adds the given tab.
	 */
	operator fun <T : TabNavigatorTab> T.unaryPlus(): T {
		return addTab(tabs.size, this)
	}

	/**
	 * Removes the given tab.
	 */
	operator fun <T : TabNavigatorTab> T.unaryMinus(): T {
		removeTab(this)
		return this
	}

	fun <T : TabNavigatorTab> addTab(tab: T): T = addTab(tabs.size, tab)

	/**
	 * Adds the tab to the given index. If the tab is already added, it will be removed first and added to the new
	 * index.
	 */
	fun <T : TabNavigatorTab> addTab(index: Int, tab: T): T {
		// TODO: Handle tab reorder without removal.
		if (tab.isDisposed) throw Exception("Tab is disposed.")
		var newIndex = index
		val oldIndex = tabs.indexOf(tab)
		if (oldIndex != -1) {
			if (newIndex == oldIndex || newIndex == oldIndex + 1) return tab // Element was added in the same spot it previously was.
			// Handle the case where after the element is removed, the new index needs to decrement to compensate.
			if (oldIndex < newIndex)
				newIndex--
			removeTab(oldIndex)
		}

		if (newIndex == 0) {
			if (_tabs.size > 0) {
				_tabs[0].button.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
			}
			tab.button.styleTags.add(DEFAULT_TAB_STYLE_FIRST)
		}
		if (newIndex == tabs.size) {
			if (_tabs.size > 0) {
				_tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
			}
			tab.button.styleTags.add(DEFAULT_TAB_STYLE_LAST)
		}
		tab.button.styleTags.add(DEFAULT_TAB_STYLE)

		_tabs.add(newIndex, tab)
		tabBar.addElement(newIndex, tab.button)
		tab.button.click().add(::tabClickHandler)

		updateSelectedTab()
		tab.disposed.add(::tabDisposedHandler)
		return tab
	}

	/**
	 * Removes the given tab.
	 * @param tab The tab to remove. If this is not in the _tabs list, an error will be thrown.
	 */
	fun removeTab(tab: TabNavigatorTab?): Boolean {
		if (tab == null) return false
		val index = _tabs.indexOf(tab)
		if (index == -1) return false
		removeTab(index)
		return true
	}

	/**
	 * Returns the tab at the given index.
	 * @param index Between 0 and `tabs.lastIndex`
	 * @return Returns the removed tab.
	 * @throws IndexOutOfBoundsException
	 */
	fun removeTab(index: Int): TabNavigatorTab {
		val r = _tabs.removeAt(index)
		val t = r.button
		if (index == 0) {
			if (_tabs.size > 0) {
				styleTags.add(DEFAULT_TAB_STYLE_FIRST)
			}
			t.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
		}
		if (index == _tabs.size) {
			if (_tabs.size > 0) {
				_tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
			}
			t.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
		}
		t.click().remove(::tabClickHandler)
		tabBar.removeElement(r.button)

		updateSelectedTab()
		r.disposed.remove(::tabDisposedHandler)
		return r
	}

	fun setTabLabel(index: Int, newLabel: String) {
		_tabs[index].button.label = newLabel.orSpace()
	}

	fun clearTabs(dispose: Boolean = true) {
		if (!_tabs.isNotEmpty()) return
		_tabs.first().button.styleTags.remove(DEFAULT_TAB_STYLE_FIRST)
		_tabs.last().button.styleTags.remove(DEFAULT_TAB_STYLE_LAST)
		if (dispose) {
			while (_tabs.isNotEmpty())
				_tabs.last().dispose()
		} else {
			tabBar.clearElements(dispose = false)
			_tabs.clear()
		}
		updateSelectedTab()
	}

	private fun tabDisposedHandler(tab: Context) {
		removeTab(tab as TabNavigatorTab)
	}

	//-----------------------------------------------------

	private fun updateSelectedTab() {
		val newSelectedTab: TabNavigatorTab? = if (_currentIndex >= 0 && _currentIndex < _tabs.size) {
			_tabs[_currentIndex]
		} else {
			null
		}
		val lastSelectedTab = selectedTab
		if (newSelectedTab != lastSelectedTab) {
			selectedTab = newSelectedTab
			if (lastSelectedTab != null && !lastSelectedTab.isDisposed) {
				lastSelectedTab.button.toggled = false
				contents.removeElement(lastSelectedTab.content.instance)
			}
			if (newSelectedTab != null) {
				contents.addElement(newSelectedTab.content.instance)
				newSelectedTab.button.toggled = true
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val background = background ?: return
		updateTabBarLayout(explicitWidth, explicitHeight)
		val contentsHeight = if (explicitHeight == null) null else explicitHeight - tabBarHeight
		val pad = style.contentsPadding
		contents.size(pad.reduceWidth(explicitWidth), pad.reduceHeight(contentsHeight))
		contents.position(pad.left, tabBarHeight + pad.top)
		background.size(pad.expandWidth(contents.width), pad.expandHeight(contents.height))
		background.position(0f, tabBarHeight)
		out.width = maxOf(background.width, tabBarWidth)
		out.height = background.height + tabBarHeight
	}

	protected open fun updateTabBarLayout(explicitWidth: Float?, explicitHeight: Float?) {
		val tabPadding = style.tabBarPadding
		_tabBarContainer.size(tabPadding.reduceWidth(explicitWidth), null)
		_tabBarContainer.position(tabPadding.left, tabPadding.top)
		this.tabBarHeight = tabPadding.expandHeight(_tabBarContainer.height)
		this.tabBarWidth = tabPadding.expandWidth(_tabBarContainer.width)
	}

	override fun dispose() {
		clearTabs(dispose = false) // The tabs this component owns will be disposed in the disposed signal.
		super.dispose()
		_currentIndexChanged.dispose()
	}

	companion object : StyleTag {
		val DEFAULT_TAB_STYLE = styleTag()
		val DEFAULT_TAB_STYLE_FIRST = styleTag()
		val DEFAULT_TAB_STYLE_LAST = styleTag()
	}
}

interface TabNavigatorTab : Context, Disposable, LayoutDataProvider<StackLayoutData> {
	val button: Button
	val content: LazyInstance<Context, UiComponent>
}

class TabNavigatorTabImpl<S : Button, T : UiComponent>(
		owner: Context,
		buttonFactory: TabNavigatorTab.() -> S,
		contentFactory: TabNavigatorTab.() -> T
) : ContextImpl(owner), TabNavigatorTab {
	override val button: S = buttonFactory()
	override val content: LazyInstance<TabNavigatorTab, T> = lazyInstance(contentFactory)

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	val instance: T
		get() = content.instance
}

class TabNavigatorStyle : StyleBase() {

	override val type: StyleType<TabNavigatorStyle> = TabNavigatorStyle

	/**
	 * The horizontal gap between tabs.
	 */
	var tabGap by prop(0f)

	/**
	 * The horizontal alignment of the tabs within the tab bar.
	 */
	var tabsHorizontalAlign by prop(HAlign.LEFT)

	/**
	 * The padding around the tabs.
	 */
	var tabBarPadding by prop(Pad(0f, 0f, -1f, 0f))

	/**
	 * The component to be placed in the behind the contents.
	 */
	var background by prop(noSkin)

	/**
	 * The padding around the contents area.
	 */
	var contentsPadding by prop(Pad(0f))

	companion object : StyleType<TabNavigatorStyle>
}


fun <S : ButtonImpl, T : UiComponent> Context.tab(buttonFactory: (@ComponentDslMarker TabNavigatorTab).() -> S, contentFactory: (@ComponentDslMarker TabNavigatorTab).() -> T) = TabNavigatorTabImpl(this, buttonFactory, contentFactory)

fun <T : UiComponent> Context.tab(label: String, contentFactory: (@ComponentDslMarker TabNavigatorTab).() -> T) = tab({ button(label.orSpace()) }, contentFactory)
fun <T : UiComponent> Context.tabI18n(key: String, bundleName: String = i18nBundleName, contentFactory: (@ComponentDslMarker TabNavigatorTab).() -> T) = tab(
		{
			button {
				labelI18n(key, bundleName)
			}
		}, contentFactory)

inline fun Context.tabNavigator(init: ComponentInit<TabNavigator> = {}): TabNavigator {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TabNavigator(this)
	t.init()
	return t
}

private fun String.orSpace(): String {
	return if (this == "") "\u00A0" else this
}
