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

package com.acornui.js

import com.acornui.JsApplicationBase
import com.acornui.MainContext
import com.acornui.asset.Loaders
import com.acornui.audio.AudioManager
import com.acornui.audio.Music
import com.acornui.audio.SoundFactory
import com.acornui.browser.Location
import com.acornui.component.HtmlComponent
import com.acornui.cursor.CursorManager
import com.acornui.di.Context
import com.acornui.di.contextKey
import com.acornui.focus.FocusManager
import com.acornui.graphic.Window
import com.acornui.input.*
import com.acornui.io.*
import com.acornui.js.audio.*
import com.acornui.js.cursor.JsCursorManager
import com.acornui.js.input.JsClipboard
import com.acornui.js.input.JsKeyInput
import com.acornui.js.input.JsMouseInput
import com.acornui.js.window.JsLocation
import com.acornui.uncaughtExceptionHandler
import org.w3c.dom.HTMLElement
import kotlin.browser.window
import kotlin.time.seconds

/**
 * The base class for browser-based Acorn UI applications.
 * This will add boot tasks that initialize input for the target canvas.
 */
@Suppress("unused")
abstract class BrowserApplicationBase(mainContext: MainContext) : JsApplicationBase(mainContext) {

	init {
		val window = if (jsTypeOf(window) != "undefined") window else error("BrowserApplicationBase can only be used in browser applications.")

		// Uncaught exception handler
		val prevOnError = window.onerror
		window.onerror = { message, source, lineNo, colNo, error ->
			prevOnError?.invoke(message, source, lineNo, colNo, error)
			if (error is Throwable)
				uncaughtExceptionHandler(error)
			else
				uncaughtExceptionHandler(Exception("Unknown error: $message $lineNo $source $colNo $error"))
		}

		val oBU = window.onbeforeunload
		window.onbeforeunload = {
			oBU?.invoke(it)
			dispose()
			undefined // Necessary for ie11 not to alert user.
		}
	}

	abstract val canvasTask: suspend () -> HTMLElement
	abstract val windowTask: suspend () -> Window
	abstract val componentsTask: suspend () -> (owner: Context) -> HtmlComponent

	protected open val location by task(Location) {
		JsLocation(window.location)
	}

	protected open val mouseInputTask by task(MouseInput) {
		JsMouseInput(get(CANVAS))
	}

	protected open val keyInputTask by task(KeyInput) {
		JsKeyInput(get(CANVAS))
	}

	protected open val interactivityTask by task(InteractivityManager) {
		InteractivityManagerImpl(get(MouseInput), get(KeyInput), get(FocusManager))
	}

	protected open val cursorManagerTask by task(CursorManager) {
		JsCursorManager(get(CANVAS))
	}

	protected open val soundLoaderTask by task(Loaders.soundLoader) {
		val defaultSettings = get(defaultRequestSettingsKey)
		val audioContextSupported = audioContextSupported
		val audioManager = get(AudioManager)
		object : Loader<SoundFactory> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = Bandwidth.downBpsInv.seconds * 100_000)

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): SoundFactory {
				return if (audioContextSupported) {
					loadAudioSound(audioManager, requestData, settings)
				} else {
					loadAudioElement(audioManager, requestData, settings)
				}
			}
		}
	}

	protected open val musicLoaderTask by task(Loaders.musicLoader) {
		val defaultSettings = get(defaultRequestSettingsKey)
		val audioManager = get(AudioManager)
		object : Loader<Music> {
			override val requestSettings: RequestSettings =
					defaultSettings.copy(initialTimeEstimate = 0.seconds) // Audio element is immediately returned.

			override suspend fun load(requestData: UrlRequestData, settings: RequestSettings): Music {
				return JsAudioElementMusic(audioManager, Audio(requestData.toUrlStr(settings.rootPath)))
			}
		}
	}

	protected open val clipboardTask by task(Clipboard) {
		JsClipboard(
				get(CANVAS),
				get(FocusManager),
				get(InteractivityManager)
		)
	}

	companion object {
		protected val CANVAS = contextKey<HTMLElement>()
	}

}