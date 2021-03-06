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

package com.acornui.dom

import com.acornui.signal.event
import org.w3c.dom.Document
import org.w3c.dom.events.Event

inline val Document.hidden: Boolean
	get() = asDynamic().hidden

@Deprecated("Use visibilityChanged", ReplaceWith("this.visibilityChanged"))
val Document.visibilityChange
	get() = visibilityChanged

val Document.visibilityChanged
	get() = event<Event>("visibilitychange")

val Document.selectionChanged
	get() = event<Event>("selectionchange")