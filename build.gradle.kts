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

val GRADLE_VERSION: String by extra
val PRODUCT_VERSION: String by extra
val PRODUCT_GROUP: String by extra
allprojects {
    repositories {
        jcenter()
    }

    tasks.withType<Wrapper> {
        gradleVersion = GRADLE_VERSION
        distributionType = Wrapper.DistributionType.ALL
    }

    tasks.withType(Delete::class.java)
            .matching {
                it.name == BasePlugin.CLEAN_TASK_NAME
            }
            .all {
                delete(fileTree(".").matching {
                    include("**/out/", "**/dist/", "**/www", "**/wwwDist")
                })
            }

    version = PRODUCT_VERSION
    group = PRODUCT_GROUP
}