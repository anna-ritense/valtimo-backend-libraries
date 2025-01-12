/*
 * Copyright 2015-2024 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.outbox.domain

import com.fasterxml.jackson.databind.node.ContainerNode
import java.time.LocalDateTime
import java.util.UUID

abstract class BaseEvent(
    val id: UUID = UUID.randomUUID(),
    val type: String,
    val date: LocalDateTime = LocalDateTime.now(),
    val userId: String? = null,
    val roles: Set<String> = setOf(),
    val resultType: String?,
    val resultId: String?,
    val result: ContainerNode<*>?,
)