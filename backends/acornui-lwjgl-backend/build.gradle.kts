/*
 * Copyright 2019 PolyForest
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

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val KOTLIN_LANGUAGE_VERSION: String by extra
val KOTLIN_JVM_TARGET: String by extra
kotlin {
    js {
        compilations.named("test") {
            runtimeDependencyFiles
        }
        compilations.all {
            kotlinOptions {
                moduleKind = "amd"
                sourceMap = true
                sourceMapEmbedSources = "always"
                main = "noCall"
            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = KOTLIN_JVM_TARGET
            }
        }
    }
    targets.all {
        compilations.all {
            kotlinOptions {
                languageVersion = KOTLIN_LANGUAGE_VERSION
                apiVersion = KOTLIN_LANGUAGE_VERSION
                verbose = true
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":acornui-core"))
                implementation(project(":acornui-utils"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        named("jvmMain") {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))

                val LWJGL_VERSION: String by extra
                val JORBIS_VERSION: String by extra
                val JLAYER_VERSION: String by extra
                val lwjglGroup = "org.lwjgl"
                val lwjglName = "lwjgl"
                val natives = arrayOf("windows", "macos", "linux")
                val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")

                implementation("$lwjglGroup:$lwjglName:$LWJGL_VERSION")
                extensions.forEach { implementation("$lwjglGroup:$lwjglName-$it:$LWJGL_VERSION") }
                implementation("com.badlogicgames.jlayer:jlayer:$JLAYER_VERSION-gdx")
                implementation("org.jcraft:jorbis:$JORBIS_VERSION")

                for (native in natives) {
                    runtimeOnly("$lwjglGroup:$lwjglName:$LWJGL_VERSION:natives-$native")
                    extensions.forEach { runtimeOnly("$lwjglGroup:$lwjglName-$it:$LWJGL_VERSION:natives-$native") }
                }
            }
        }
        named("jvmTest") {
            dependencies {
                val HAMCREST_VERSION: String by extra
                val MOCKITO_VERSION: String by extra
                val OBJENESIS_VERSION: String by extra
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("org.hamcrest:hamcrest-core:$HAMCREST_VERSION")
                implementation("org.mockito:mockito-core:$MOCKITO_VERSION")
                implementation("org.objenesis:objenesis:$OBJENESIS_VERSION")

            }
        }
        named("jsMain") {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        named("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}