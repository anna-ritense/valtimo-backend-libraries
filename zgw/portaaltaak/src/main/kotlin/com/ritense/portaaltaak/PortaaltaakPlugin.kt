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

package com.ritense.portaaltaak

import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.logging.withLoggingContext
import com.ritense.notificatiesapi.NotificatiesApiPlugin
import com.ritense.plugin.annotation.Plugin
import com.ritense.plugin.annotation.PluginAction
import com.ritense.plugin.annotation.PluginActionProperty
import com.ritense.plugin.annotation.PluginProperty
import com.ritense.portaaltaak.domain.TaakVersion
import com.ritense.portaaltaak.service.PortaaltaakService
import com.ritense.processlink.domain.ActivityTypeWithEventName
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import java.util.UUID

@Plugin(
    key = "portaaltaak",
    title = "Portaaltaak",
    description = "Enable interfacing with Portaaltaak specification compliant APIs"
)
class PortaaltaakPlugin(
    private val portaaltaakService: PortaaltaakService
) {
    @PluginProperty(key = "notificatiesApiPluginConfiguration", secret = false)
    lateinit var notificatiesApiPluginConfiguration: NotificatiesApiPlugin

    @PluginProperty(key = "objectManagementConfigurationId", secret = false)
    lateinit var objectManagementConfigurationId: UUID

    @PluginProperty(key = "taakVersion", secret = false)
    var taakVersion: TaakVersion = TaakVersion.V1

    @PluginProperty(key = "completeTaakProcess", secret = false)
    lateinit var completeTaakProcess: String

    @PluginAction(
        key = "create-portaaltaak",
        title = "Create portal task",
        description = "Create a task for a portal by storing it in the Objecten-API",
        activityTypes = [ActivityTypeWithEventName.USER_TASK_CREATE]
    )
    fun createPortaalTaak(
        delegateTask: DelegateTask,
        @PluginActionProperty config: ObjectNode,
    ) {
        withLoggingContext(DelegateTask::class.java.canonicalName to delegateTask.id) {
            portaaltaakService.createPortaaltaak(
                objectManagementId = objectManagementConfigurationId,
                version = taakVersion,
                config = config,
                delegateTask = delegateTask
            )
        }
    }

    @PluginAction(
        key = "complete-portaaltaak",
        title = "Complete Portaaltaak",
        description = "Complete portal task and update status on Objects Api",
        activityTypes = [ActivityTypeWithEventName.SERVICE_TASK_START]
    )
    fun completePortaalTaak(execution: DelegateExecution) {
        withLoggingContext(DelegateExecution::class.java.canonicalName to execution.id) {
            portaaltaakService.completePortaaltaak(
                objectManagementId = objectManagementConfigurationId,
                version = taakVersion,
                execution = execution,
            )
        }
    }

}