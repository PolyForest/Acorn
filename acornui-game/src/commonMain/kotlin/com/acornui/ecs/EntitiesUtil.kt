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

package com.acornui.ecs

import com.acornui.di.Context
import com.acornui.mvc.Command
import com.acornui.mvc.CommandType
import com.acornui.mvc.Commander
import com.acornui.mvc.invokeCommand

class AddEntity(val entity: Entity) : Command {

	override val type = Companion

	companion object : CommandType<AddEntity>
}

class RemoveEntity(val entity: Entity) : Command {

	override val type = Companion

	companion object : CommandType<RemoveEntity>
}

inline fun watchEntities(entities: List<Entity>, cmd: Commander, crossinline initEntity: (e: Entity) -> Unit, crossinline disposeEntity: (Entity) -> Unit) {
	for (i in 0..entities.lastIndex) {
		val e = entities[i]
		initEntity(e)
	}
	cmd.onCommandInvoked(AddEntity) {
		initEntity(it.entity)
	}
	cmd.onCommandInvoked(RemoveEntity) {
		disposeEntity(it.entity)
	}
}

fun Context.addEntity(e: Entity) {
	invokeCommand(AddEntity(e))
}

fun Context.addEntity(init: Entity.() -> Unit) {
	val e = entity()
	e.init()
	invokeCommand(AddEntity(e))
}

fun Context.removeEntity(e: Entity) {
	invokeCommand(RemoveEntity(e))
}

fun entitiesList(entities: List<Entity>, cmd: Commander, filtered: MutableList<Entity>, componentType: ComponentType<*>) {
	watchEntities(entities, cmd, {
		if (it.containsComponent(componentType)) {
			filtered.add(it)
		}
	}, {
		if (it.containsComponent(componentType)) {
			filtered.remove(it)
		}
	})
}

fun <T : Component> componentList(entities: List<Entity>, cmd: Commander, filtered: MutableList<T>, componentType: ComponentType<T>) {
	watchEntities(entities, cmd, {
		if (it.containsComponent(componentType)) {
			filtered.add(it.getComponent(componentType))
		}
	}, {
		if (it.containsComponent(componentType)) {
			filtered.remove(it.getComponent(componentType))
		}
	})
}
