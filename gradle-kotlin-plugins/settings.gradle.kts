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

pluginManagement {
	val kotlinVersion: String by settings
	repositories {
		mavenLocal()
		gradlePluginPortal()

		maven {
			url = uri("https://dl.bintray.com/kotlin/kotlin-eap/")
		}
	}
	resolutionStrategy {
		eachPlugin {
			when {
				requested.id.namespace == "org.jetbrains.kotlin" ->
					useVersion(kotlinVersion)
			}
		}
	}
	buildscript {
		dependencies {
			classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$kotlinVersion")
		}
		repositories {
			mavenLocal()
			gradlePluginPortal()
			maven {
				url = uri("https://dl.bintray.com/kotlin/kotlin-eap/")
			}
		}
	}
}