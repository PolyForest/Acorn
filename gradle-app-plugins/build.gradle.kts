@file:Suppress("UnstableApiUsage")

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

import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension

plugins {
	`maven-publish`
	`java-gradle-plugin`
	kotlin("jvm")
}

buildscript {
	val kotlinVersion: String by project
	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$kotlinVersion")
	}
}

apply(plugin = "kotlin-sam-with-receiver")

samWithReceiver {
	annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.samWithReceiver(configure: SamWithReceiverExtension.() -> Unit): Unit = extensions.configure("samWithReceiver", configure)

val kotlinVersion: String by project
val kotlinSerializationVersion: String by project
val dokkaVersion: String by project

dependencies {
	compileOnly(gradleKotlinDsl())
	compileOnly(gradleApi())
	implementation(kotlin("compiler", version = kotlinVersion))
	implementation(kotlin("gradle-plugin", version = kotlinVersion))
	implementation(kotlin("serialization", version = kotlinVersion))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
//	implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

	implementation("com.acornui:gradle-kotlin-plugins:$version")
	implementation(project(":acornui-utils"))
	implementation(project(":acornui-core"))
	implementation(project(":acornui-lwjgl-backend"))
	implementation(project(":acornui-texture-packer"))
	implementation(project(":gdx-font-processor"))

	testImplementation(gradleKotlinDsl())
	testImplementation(gradleTestKit())
	testImplementation(kotlin("test", version = kotlinVersion))
	testImplementation(kotlin("test-junit", version = kotlinVersion))
}

val kotlinLanguageVersion: String by project
val kotlinJvmTarget: String by project

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
	sourceSets.configureEach {
		languageSettings.useExperimentalAnnotation("kotlin.Experimental")
	}
	target {
		compilations.configureEach {
			kotlinOptions {
				jvmTarget = kotlinJvmTarget
				languageVersion = kotlinLanguageVersion
				apiVersion = kotlinLanguageVersion
			}
		}
	}
}

gradlePlugin {
	plugins {
		create("root-settings") {
			id = "com.acornui.settings"
			implementationClass = "com.acornui.build.plugins.SettingsPlugin"
			displayName = "Settings plugin for faux composite builds with Acorn UI."
			description = """Creates a faux composite build with Acorn UI using the project property acornUiHome. 
|If property acornUiHome is not set, this plugin will do nothing.
|Acorn dependencies should be declared using `com.acornui.build.plugins.util.acorn`. E.g.: 
|```
|dependencies {
|  implementation(acorn(project, "utils"))
|}
|```
|This is a workaround to issue KT-30285.""".trimMargin()
		}
		create("root") {
			id = "com.acornui.root"
			implementationClass = "com.acornui.build.plugins.RootPlugin"
			displayName = "Root project plugin for an Acorn UI application."
			description = "Configuration of a root project for an Acorn UI application."
		}
		create("app") {
			id = "com.acornui.app"
			implementationClass = "com.acornui.build.plugins.AcornUiApplicationPlugin"
			displayName = "Acorn UI Multi-Platform Application"
			description = "Configuration of an Acorn UI Application project. Plugin \"com.acornui.root\" should first be applied to the root project."
		}
	}
}

tasks.named<ProcessResources>("processResources").configure {
	filesMatching("acorn.properties") {
		expand(project.properties)
	}
}

tasks.named<ProcessResources>("processTestResources").configure {
	filesMatching("**/gradle.properties") {
		expand(project.properties)
	}
}