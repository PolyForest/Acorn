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

rootProject.name = "acornui-skins"

pluginManagement {
	val version: String by settings
	repositories {
		mavenLocal()
		maven {
			url = uri("https://maven.pkg.github.com/polyforest/acornui")
			credentials {
				username = "anonymous"
				password = "a1b92e4b7ff208be2b7f0f8524bb2a48566079f9"
			}
		}
		gradlePluginPortal()
		maven {
			url = uri("https://dl.bintray.com/kotlin/kotlin-eap/")
		}
	}
	resolutionStrategy {
		eachPlugin {
			when {
				requested.id.namespace == "com.acornui" ->
					useVersion(version)
			}
		}
	}
}

include("basic")