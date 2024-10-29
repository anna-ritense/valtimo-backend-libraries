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

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.ritense.authorization.AuthorizationContext.Companion.runWithoutAuthorization
import com.ritense.authorization.annotation.RunWithoutAuthorization
import com.ritense.notificatiesapi.event.NotificatiesApiNotificationReceivedEvent
import com.ritense.notificatiesapi.exception.NotificatiesNotificationEventException
import com.ritense.objectenapi.ObjectenApiPlugin
import com.ritense.objectmanagement.domain.ObjectManagement
import com.ritense.objectmanagement.service.ObjectManagementService
import com.ritense.plugin.domain.PluginConfiguration
import com.ritense.plugin.domain.PluginConfigurationId
import com.ritense.plugin.service.PluginService
import com.ritense.portaaltaak.domain.DataBindingConfig
import com.ritense.portaaltaak.domain.TaakObject
import com.ritense.portaaltaak.domain.TaakObjectV2
import com.ritense.portaaltaak.domain.TaakStatus
import com.ritense.portaaltaak.domain.TaakVersion
import com.ritense.processdocument.domain.ProcessInstanceId
import com.ritense.processdocument.domain.impl.CamundaProcessInstanceId
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.valtimo.camunda.domain.CamundaTask
import com.ritense.valtimo.service.CamundaProcessService
import com.ritense.valtimo.service.CamundaTaskService
import com.ritense.valueresolver.ValueResolverService
import mu.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.VariableScope
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional
import java.net.MalformedURLException
import java.net.URI
import java.util.UUID

open class PortaalTaakEventListener(
    private val objectManagementService: ObjectManagementService,
    private val pluginService: PluginService,
    private val processDocumentService: ProcessDocumentService,
    private val processService: CamundaProcessService,
    private val taskService: CamundaTaskService,
    private val runtimeService: RuntimeService,
    private val valueResolverService: ValueResolverService,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    @RunWithoutAuthorization
    @EventListener(NotificatiesApiNotificationReceivedEvent::class)
    open fun processCompletePortaalTaakEvent(event: NotificatiesApiNotificationReceivedEvent) {
        logger.debug { "Received Notificaties API event, checking if it fits criteria to complete a portaaltaak" }

        val objectType = event.kenmerken["objectType"]
        if (!event.kanaal.equals("objecten", ignoreCase = true) ||
            !event.actie.equals("update", ignoreCase = true) ||
            objectType == null
        ) {
            logger.debug { "Notificaties API event does not match criteria for completing a portaaltaak. Ignoring." }
            return
        }

        val objectTypeId = objectType.substringAfterLast("/")

        val objectManagement =
            objectManagementService.findByObjectTypeId(objectTypeId)
                ?: run {
                    logger.warn { "Object management not found for object type id '$objectTypeId'" }
                    return
                }

        pluginService.findPluginConfiguration(PortaaltaakPlugin::class.java) { properties: JsonNode ->
            properties.get("objectManagementConfigurationId").textValue().equals(objectManagement.id.toString())
        }?.let { matchedPluginConfig: PluginConfiguration ->
            logger.debug { "Completing portaaltaak using plugin configuration with id '${matchedPluginConfig.id}'" }
            val pluginTaakVersion: TaakVersion =
                objectMapper.convertValue(matchedPluginConfig.properties!!.get("taakVersion"))
            when (pluginTaakVersion) {
                TaakVersion.V1 -> {
                    logger.debug {
                        "Matched Portaaltaak plugin is configured for Taak V1. Attempting to handle object as such."
                    }
                    handleResourceAsTaakV1(
                        resourceUrl = event.resourceUrl,
                        matchedPluginConfigId = matchedPluginConfig.id.id,
                        objectManagement = objectManagement,
                    )
                }

                TaakVersion.V2 -> {
                    logger.debug {
                        "Matched Portaaltaak plugin is configured for Taak V2. Attempting to handle object as such."
                    }
                    handleResourceAsTaakV2(
                        resourceUrl = event.resourceUrl,
                        matchedPluginConfigId = matchedPluginConfig.id.id,
                        objectManagement = objectManagement,
                    )
                }
            }

        }
            ?: logger.warn { "No portaaltaak plugin configuration found with object management configuration id '${objectManagement.id}'" }
    }

    private fun handleResourceAsTaakV1(
        resourceUrl: String,
        matchedPluginConfigId: UUID,
        objectManagement: ObjectManagement,
    ) {
        val taakObject: TaakObject =
            try {
                objectMapper.convertValue(getPortaalTaakObjectData(objectManagement, resourceUrl))
            } catch (ex: Exception) {
                logger.debug {
                    "Failed to handle object ${resourceUrl.substringAfterLast("/")} as Taak V1. " +
                        "Is the correct Object Management configuration linked to this Portaaltaak plugin?"
                }
                throw ex
            }
        when (taakObject.status) {
            TaakStatus.INGEDIEND -> {
                logger.debug { "Processing task with status 'ingediend' and verwerker task id '${taakObject.verwerkerTaakId}'" }
                val task = taskService.findTaskById(taakObject.verwerkerTaakId)
                    ?: run {
                        logger.warn { "Task not found with verwerker task id '${taakObject.verwerkerTaakId}'" }
                        return
                    }

                val receiveData = getReceiveDataActionProperty(task, matchedPluginConfigId) ?: return

                val portaaltaakPlugin: PortaaltaakPlugin = pluginService.createInstance(matchedPluginConfigId)
                val processInstanceId = CamundaProcessInstanceId(task.getProcessInstanceId())
                val documentId = runWithoutAuthorization {
                    processDocumentService.getDocumentId(processInstanceId, task)
                }
                saveDataInDocument(taakObject.verzondenData, task, receiveData)
                startProcessToUploadDocuments(
                    processDefinitionKey = portaaltaakPlugin.completeTaakProcess,
                    businessKey = documentId.id.toString(),
                    portaalTaakObjectUrl = resourceUrl,
                    objectenApiPluginConfigurationId = objectManagement.objectenApiPluginConfigurationId.toString(),
                    verwerkerTaakId = taakObject.verwerkerTaakId,
                    documentUrls = getDocumentenUrls(objectMapper.convertValue(taakObject.verzondenData)),
                    taakVersion = TaakVersion.V1
                )
            }

            else -> {
                logger.debug { "Taak status is not 'ingediend', skipping completion of portaaltaak" }
            }
        }
    }

    private fun handleResourceAsTaakV2(
        resourceUrl: String,
        matchedPluginConfigId: UUID,
        objectManagement: ObjectManagement,
    ) {
        val taakObject: TaakObjectV2 =
            try {
                objectMapper.convertValue(getPortaalTaakObjectData(objectManagement, resourceUrl))
            } catch (ex: Exception) {
                logger.debug {
                    "Failed to handle object ${resourceUrl.substringAfterLast("/")} as Taak V2. " +
                        "Is the correct Object Management configuration linked to this Portaaltaak plugin?"
                }
                throw ex
            }
        when (taakObject.status) {
            TaakObjectV2.TaakStatus.AFGEROND -> {
                logger.debug { "Processing Task V2 with status 'afgerond' and verwerker task id '${taakObject.verwerkerTaakId}'" }
                val task = taskService.findTaskById(taakObject.verwerkerTaakId.toString())
                    ?: run {
                        logger.warn { "Task not found with verwerker task id '${taakObject.verwerkerTaakId}'" }
                        return
                    }

                val receiveData = getReceiveDataActionProperty(task, matchedPluginConfigId) ?: return

                val portaaltaakPlugin: PortaaltaakPlugin = pluginService.createInstance(matchedPluginConfigId)
                val processInstanceId = CamundaProcessInstanceId(task.getProcessInstanceId())
                val documentId = runWithoutAuthorization {
                    processDocumentService.getDocumentId(processInstanceId, task)
                }
                if (taakObject.soort == TaakObjectV2.TaakSoort.PORTAALFORMULIER) {
                    saveDataInDocument(taakObject.portaalformulier!!.verzondenData, task, receiveData)
                }
                startProcessToUploadDocuments(
                    processDefinitionKey = portaaltaakPlugin.completeTaakProcess,
                    businessKey = documentId.id.toString(),
                    portaalTaakObjectUrl = resourceUrl,
                    objectenApiPluginConfigurationId = objectManagement.objectenApiPluginConfigurationId.toString(),
                    verwerkerTaakId = taakObject.verwerkerTaakId.toString(),
                    documentUrls = getDocumentenUrls(objectMapper.convertValue(taakObject.portaalformulier!!.verzondenData)),
                    taakVersion = TaakVersion.V2
                )
            }

            else -> {
                logger.debug { "Taak status is not 'ingediend', skipping completion of portaaltaak" }
            }
        }
    }

    private fun getReceiveDataActionProperty(task: CamundaTask, pluginConfigurationId: UUID): List<DataBindingConfig>? {
        logger.debug { "Retrieving receive data action property for task with id '${task.id}'" }
        val processLinks = pluginService.getProcessLinks(task.getProcessDefinitionId(), task.taskDefinitionKey!!)
        return processLinks
            .firstOrNull { processLink ->
                processLink.pluginConfigurationId == pluginConfigurationId
            }
            ?.let { processLink ->
                val actionTaakVersion: TaakVersion =
                    objectMapper.convertValue(processLink.actionProperties!!.get("taakVersion"))
                val actionConfig = processLink.actionProperties!!.get("config")

                val receiveDataJsonNode = when (actionTaakVersion) {
                    TaakVersion.V1 -> {
                        actionConfig?.get("receiveData") ?: run {
                            logger.warn { "No receive data for task with id '${task.id}'" }
                            null
                        }
                    }

                    TaakVersion.V2 -> {
                        actionConfig?.get("portaalformulierVerzondenData") ?: run {
                            logger.warn { "No receive data for task with id '${task.id}'" }
                            return null
                        }
                    }
                }

                return receiveDataJsonNode?.let { objectMapper.treeToValue(receiveDataJsonNode) }
            }

    }

    private fun saveDataInDocument(
        submittedData: Map<String, Any>,
        task: CamundaTask,
        receiveData: List<DataBindingConfig>
    ) {
        logger.debug { "Saving data in document for task with id '${task.id}'" }
        if (submittedData.isNotEmpty()) {
            val processInstanceId = CamundaProcessInstanceId(task.getProcessInstanceId())
            val variableScope = getVariableScope(task)
            val taakObjectData = objectMapper.valueToTree<JsonNode>(submittedData)
            val resolvedValues = getResolvedValues(receiveData, taakObjectData)
            handleTaakObjectData(processInstanceId, variableScope, resolvedValues)
        } else {
            logger.warn { "No data found in taakobject for task with id '${task.id}'" }
        }
    }

    /**
     * @param receiveData: [ doc:/streetName  to  "/persoonsData/adres/straatnaam" ]
     * @param data {"persoonsData":{"adres":{"straatnaam":"Funenpark"}}}
     * @return mapOf(doc:/streetName to "Funenpark")
     */
    private fun getResolvedValues(receiveData: List<DataBindingConfig>, data: JsonNode): Map<String, Any> {
        return receiveData.associateBy({ it.key }, { getValue(data, it.value) })
    }

    private fun getValue(data: JsonNode, path: String): Any {
        val valueNode = data.at(JsonPointer.valueOf(path))
        if (valueNode.isMissingNode) {
            throw RuntimeException("Failed to find path '$path' in data: \n${data.toPrettyString()}")
        }
        return objectMapper.treeToValue(valueNode, Object::class.java)
    }

    private fun handleTaakObjectData(
        processInstanceId: ProcessInstanceId,
        variableScope: VariableScope,
        resolvedValues: Map<String, Any>
    ) {
        if (resolvedValues.isNotEmpty()) {
            valueResolverService.handleValues(processInstanceId.toString(), variableScope, resolvedValues)
        }
    }

    private fun getVariableScope(task: CamundaTask): VariableScope {
        return runtimeService.createProcessInstanceQuery()
            .processInstanceId(task.getProcessInstanceId())
            .singleResult() as VariableScope
    }

    private fun getDocumentenUrls(verzondenData: JsonNode): List<String> {
        val documentPathsNode = verzondenData.at(JsonPointer.valueOf("/documenten"))
        if (documentPathsNode.isMissingNode || documentPathsNode.isNull) {
            return emptyList()
        }
        if (!documentPathsNode.isArray) {
            throw NotificatiesNotificationEventException(
                "Could not retrieve document Urls.'/documenten' is not an array"
            )
        }
        val documentenUris = mutableListOf<String>()
        for (documentPathNode in documentPathsNode) {
            val documentUrlNode = verzondenData.at(JsonPointer.valueOf(documentPathNode.textValue()))
            if (!documentUrlNode.isMissingNode && !documentUrlNode.isNull) {
                try {
                    if (documentUrlNode.isTextual) {
                        documentenUris.add(documentUrlNode.textValue())
                    } else if (documentUrlNode.isArray) {
                        documentUrlNode.forEach { documentenUris.add(it.textValue()) }
                    } else {
                        throw NotificatiesNotificationEventException(
                            "Could not retrieve document Urls. Found invalid URL in '/documenten'. ${documentUrlNode.toPrettyString()}"
                        )
                    }
                } catch (e: MalformedURLException) {
                    throw NotificatiesNotificationEventException(
                        "Could not retrieve document Urls. Malformed URL in: '/documenten'"
                    )
                }
            }
        }
        return documentenUris
    }

    internal fun startProcessToUploadDocuments(
        processDefinitionKey: String,
        businessKey: String,
        portaalTaakObjectUrl: String,
        objectenApiPluginConfigurationId: String,
        verwerkerTaakId: String,
        documentUrls: List<String>,
        taakVersion: TaakVersion,
    ) {
        logger.debug { "Starting process to upload documents for taak with verwerker task id '$verwerkerTaakId'" }
        val variables = mapOf(
            "taakVersion" to taakVersion,
            "portaalTaakObjectUrl" to portaalTaakObjectUrl,
            "objectenApiPluginConfigurationId" to objectenApiPluginConfigurationId,
            "verwerkerTaakId" to verwerkerTaakId,
            "documentUrls" to documentUrls
        )
        try {
            runWithoutAuthorization {
                processService.startProcess(processDefinitionKey, businessKey, variables)
            }
            logger.info { "Process started successfully for process definition key '$processDefinitionKey' and document id '${businessKey}'" }
        } catch (ex: RuntimeException) {
            throw NotificatiesNotificationEventException(
                "Could not start process with definition: $processDefinitionKey and businessKey: $businessKey.\n " +
                    "Reason: ${ex.message}"
            )
        }
    }

    private fun getPortaalTaakObjectData(
        objectManagement: ObjectManagement,
        resourceUrl: String
    ): JsonNode {
        logger.debug { "Retrieving portaalTaak object data for event with resource url '${resourceUrl}'" }
        val objectenApiPlugin =
            pluginService
                .createInstance(PluginConfigurationId(objectManagement.objectenApiPluginConfigurationId)) as ObjectenApiPlugin
        return objectenApiPlugin.getObject(URI(resourceUrl)).record.data
            ?: throw NotificatiesNotificationEventException(
                "Portaaltaak meta data was empty!"
            )
    }


    companion object {
        private val logger = KotlinLogging.logger {}
    }
}