/*
 * Copyright 2015-2023 Ritense BV, the Netherlands.
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

package com.ritense.processlink.service

import com.ritense.processlink.domain.ActivityTypeWithEventName
import com.ritense.processlink.domain.ProcessLink
import com.ritense.processlink.mapper.ProcessLinkMapper
import com.ritense.processlink.repository.ProcessLinkRepository
import com.ritense.processlink.web.rest.dto.ProcessLinkCreateRequestDto
import com.ritense.processlink.web.rest.dto.ProcessLinkUpdateRequestDto
import mu.KotlinLogging
import java.util.UUID
import javax.validation.ValidationException
import kotlin.jvm.optionals.getOrElse

open class ProcessLinkService(
    private val processLinkRepository: ProcessLinkRepository,
    private val processLinkMappers: List<ProcessLinkMapper>,
) {

    fun getProcessLinks(processDefinitionId: String, activityId: String): List<ProcessLink> {
        return processLinkRepository.findByProcessDefinitionIdAndActivityId(processDefinitionId, activityId)
    }

    fun getProcessLinks(activityId: String, activityType: ActivityTypeWithEventName, processLinkType: String): List<ProcessLink> {
        return processLinkRepository.findByActivityIdAndActivityTypeAndProcessLinkType(
            activityId,
            activityType,
            processLinkType
        )
    }

    fun createProcessLink(createRequest: ProcessLinkCreateRequestDto) {
        if (getProcessLinks(createRequest.processDefinitionId, createRequest.activityId).isNotEmpty()) {
            throw ValidationException("A process-link for process-definition '${createRequest.processDefinitionId}' and activity '${createRequest.activityId}' already exists!")
        }

        val mapper = getProcessLinkMapper(createRequest.processLinkType)
        processLinkRepository.save(mapper.toNewProcessLink(createRequest))
    }

    fun updateProcessLink(updateRequest: ProcessLinkUpdateRequestDto) {
        val mapper = getProcessLinkMapper(updateRequest.processLinkType)
        val processLinkToUpdate = processLinkRepository.findById(updateRequest.id)
            .getOrElse { throw IllegalStateException("No ProcessLink found with id ${updateRequest.id}") }
        val processLinkUpdated = mapper.toUpdatedProcessLink(processLinkToUpdate, updateRequest)
        processLinkRepository.save(processLinkUpdated)
    }

    fun deleteProcessLink(id: UUID) {
        processLinkRepository.deleteById(id)
    }

    private fun getProcessLinkMapper(processLinkType: String): ProcessLinkMapper {
        return processLinkMappers.singleOrNull { it.supportsProcessLinkType(processLinkType) }
            ?: throw IllegalStateException("No ProcessLinkMapper found for processLinkType $processLinkType")
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}