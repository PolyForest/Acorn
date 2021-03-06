/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui

import kotlin.test.Test
import kotlin.test.assertTrue

class DisposableBaseTest {

	@Test fun disposeRemovesOwnership() {
		val a = D()
		val b = D(a)
		b.dispose()
		a.dispose()
		assertTrue(true)
	}

	@Test fun disposeOwnerDisposesOwned() {
		val a = D()
		val b = D(a)
		a.dispose()
		assertTrue(b.isDisposed)
	}

}

private class D(owner: Owner? = null) : DisposableBase(owner), ManagedDisposable {
}
