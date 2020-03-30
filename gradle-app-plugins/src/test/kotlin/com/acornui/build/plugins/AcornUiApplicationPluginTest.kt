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

package com.acornui.build.plugins

import com.acornui.build.plugins.tasks.RunJvmTask
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AcornUiApplicationPluginTest {

	@Rule
	@JvmField
	var testProjectDir: TemporaryFolder = object : TemporaryFolder() {

		override fun before() {
			super.before()
			val projectDir = File(javaClass.getResource("/basic-acorn-project").file)
			projectDir.copyRecursively(root, overwrite = true)
		}
	}

	@Test fun addsRunJvmTask() {
		val project = ProjectBuilder.builder().build()
		project.extra["acornVersion"] = "test"
		project.pluginManager.apply("com.acornui.root")
		project.pluginManager.apply("com.acornui.app")
		assertTrue(project.tasks.getByName("runJvm") is RunJvmTask)
		assertNotNull(project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform"))
	}

	@Ignore("Latest kotlin version has unexpected build error")
	@Test fun basicAcornProject() {
		val result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments("build", "--stacktrace")
				.withPluginClasspath()
				.forwardStdOutput(System.out.bufferedWriter())
				.forwardStdError(System.err.bufferedWriter())
				.build()

		assertEquals(SUCCESS, result.task(":build")?.outcome)

		assertTrue(File(testProjectDir.root, "build/wwwProd/index.html").exists())
		assertTrue(File(testProjectDir.root, "build/wwwProd/assets/testAtlas.json").exists())
		assertTrue(File(testProjectDir.root, "build/wwwProd/assets/testAtlas0.png").exists())
		assertTrue(File(testProjectDir.root, "build/wwwProd/basic-acorn-project-production.js").exists())

		// Expect token replacement
		assertEquals("Replaced Token", File(testProjectDir.root, "build/wwwProd/assets/assetWithTokens.txt").readText())
	}
}