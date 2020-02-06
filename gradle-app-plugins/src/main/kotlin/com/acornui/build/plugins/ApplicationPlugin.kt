@file:Suppress("UnstableApiUsage", "UNUSED_VARIABLE", "EXPERIMENTAL_API_USAGE")

package com.acornui.build.plugins

import com.acornui.build.plugins.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

@Suppress("unused")
open class AcornUiApplicationPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		project.extensions.create<AcornUiApplicationExtension>("acornuiApp").apply {
			www = project.buildDir.resolve("www")
			wwwProd = project.buildDir.resolve("wwwProd")
		}
		project.pluginManager.apply(KotlinMppPlugin::class.java)
		project.extensions.configure(multiPlatformConfig(project))

		project.configureResourceProcessingTasks()
		project.configureWebTasks()
		project.configureRunJvmTask()
		project.configureUberJarTask()
	}

	private fun multiPlatformConfig(target: Project): KotlinMultiplatformExtension.() -> Unit = {
		js {
			compilations.named("main") {
				kotlinOptions {
					main = "call"
				}
			}

			browser {
				webpackTask {
					enabled = true
					val baseConventions = project.convention.plugins["base"] as BasePluginConvention?
					outputFileName =  baseConventions?.archivesBaseName + "-${mode.code}.js"
					sourceMaps = true
				}
			}
		}

		sourceSets {
			all {
				languageSettings.progressiveMode = true
			}

			val commonMain by getting {
				dependencies {
					implementation(acorn(target,"utils"))
					implementation(acorn(target,"core"))
				}
			}

			val jvmMain by getting {
				dependencies {
					implementation(acorn(target,"lwjgl-backend"))

					val lwjglVersion: String by target
					val jorbisVersion: String by target
					val jlayerVersion: String by target
					val lwjglGroup = "org.lwjgl"
					val lwjglName = "lwjgl"
					val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")

					for (os in listOf("linux", "macos", "windows")) {
						runtimeOnly("$lwjglGroup:$lwjglName:$lwjglVersion:natives-$os")
						extensions.forEach {
							runtimeOnly("$lwjglGroup:$lwjglName-$it:$lwjglVersion:natives-$os")
						}
					}
				}
			}

			val jsMain by getting {
				dependencies {
					implementation(acorn(target,"webgl-backend"))
				}
			}
		}
	}
}

open class AcornUiApplicationExtension {

	lateinit var www: File
	lateinit var wwwProd: File

	/**
	 * The directory to place the .js files.
	 * Relative to the [www] and [wwwProd] directories.
	 */
	var jsLibPath = "lib"

}

fun Project.acornuiApp(init: AcornUiApplicationExtension.() -> Unit) {
	the<AcornUiApplicationExtension>().apply(init)
}

val Project.acornuiApp
	get() : AcornUiApplicationExtension = the()