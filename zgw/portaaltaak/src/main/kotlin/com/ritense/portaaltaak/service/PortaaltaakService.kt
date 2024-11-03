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

package com.ritense.portaaltaak.service

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.ritense.authorization.AuthorizationContext.Companion.runWithoutAuthorization
import com.ritense.document.domain.patch.JsonPatchService
import com.ritense.objectenapi.ObjectenApiPlugin
import com.ritense.objectenapi.client.ObjectRecord
import com.ritense.objectenapi.client.ObjectRequest
import com.ritense.objectenapi.client.ObjectWrapper
import com.ritense.objectmanagement.domain.ObjectManagement
import com.ritense.objectmanagement.service.ObjectManagementService
import com.ritense.objecttypenapi.ObjecttypenApiPlugin
import com.ritense.plugin.domain.PluginConfigurationId
import com.ritense.plugin.service.PluginService
import com.ritense.portaaltaak.domain.CreateTaakV1ActionConfig
import com.ritense.portaaltaak.domain.CreateTaakV2ActionConfig
import com.ritense.portaaltaak.domain.DataBindingConfig
import com.ritense.portaaltaak.domain.TaakForm
import com.ritense.portaaltaak.domain.TaakFormType
import com.ritense.portaaltaak.domain.TaakFormType.ID
import com.ritense.portaaltaak.domain.TaakFormType.URL
import com.ritense.portaaltaak.domain.TaakIdentificatie
import com.ritense.portaaltaak.domain.TaakObject
import com.ritense.portaaltaak.domain.TaakObjectV2
import com.ritense.portaaltaak.domain.TaakObjectV2.OgoneBetaling
import com.ritense.portaaltaak.domain.TaakObjectV2.PortaalFormulier
import com.ritense.portaaltaak.domain.TaakObjectV2.TaakFormulier
import com.ritense.portaaltaak.domain.TaakObjectV2.TaakKoppeling
import com.ritense.portaaltaak.domain.TaakObjectV2.TaakSoort
import com.ritense.portaaltaak.domain.TaakReceiver
import com.ritense.portaaltaak.domain.TaakReceiver.OTHER
import com.ritense.portaaltaak.domain.TaakReceiver.ZAAK_INITIATOR
import com.ritense.portaaltaak.domain.TaakStatus
import com.ritense.portaaltaak.domain.TaakStatus.OPEN
import com.ritense.portaaltaak.domain.TaakStatus.VERWERKT
import com.ritense.portaaltaak.domain.TaakUrl
import com.ritense.portaaltaak.domain.TaakVersion
import com.ritense.portaaltaak.exception.CompleteTaakProcessVariableNotFoundException
import com.ritense.processdocument.domain.impl.CamundaProcessInstanceId
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.valtimo.contract.json.MapperSingleton
import com.ritense.valtimo.contract.json.patch.JsonPatchBuilder
import com.ritense.valtimo.service.CamundaTaskService
import com.ritense.valueresolver.ValueResolverService
import com.ritense.zakenapi.ZakenApiPlugin
import com.ritense.zakenapi.domain.rol.RolNatuurlijkPersoon
import com.ritense.zakenapi.domain.rol.RolNietNatuurlijkPersoon
import com.ritense.zakenapi.domain.rol.RolType
import com.ritense.zakenapi.link.ZaakInstanceLinkNotFoundException
import com.ritense.zakenapi.link.ZaakInstanceLinkService
import mu.KLogger
import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.UUID

class PortaaltaakService(
    private val objectManagementService: ObjectManagementService,
    private val pluginService: PluginService,
    private val valueResolverService: ValueResolverService,
    private val processDocumentService: ProcessDocumentService,
    private val zaakInstanceLinkService: ZaakInstanceLinkService,
    private val taskService: CamundaTaskService,
) {
    internal fun createPortaaltaak(
        version: TaakVersion,
        objectManagementId: UUID,
        config: ObjectNode,
        delegateTask: DelegateTask
    ) {
        val objectManagement = objectManagementService.getById(objectManagementId)
            ?: throw IllegalStateException("Could not find Object Management Configuration by ID $objectManagementId")


        when (version) {
            TaakVersion.V1 -> createTaskV1(
                objectManagement,
                resolveActionProperties(config, delegateTask.execution),
                delegateTask
            )

            TaakVersion.V2 -> createTaskV2(
                objectManagement,
                resolveActionProperties(config, delegateTask.execution),
                delegateTask
            )
        }
    }

    internal fun completePortaaltaak(
        version: TaakVersion,
        objectManagementId: UUID,
        delegateTask: DelegateTask
    ) {
        logger.debug { "Completing portaaltaak" }
        val execution = delegateTask.execution
        val objectManagement = objectManagementService.getById(objectManagementId)
            ?: throw IllegalStateException("Could not find Object Management Configuration by ID $objectManagementId")

        val verwerkerTaakId = (execution.getVariable("verwerkerTaakId")
            ?: throw CompleteTaakProcessVariableNotFoundException("verwerkerTaakId is required but was not provided")) as String
        val portaalTaakObjectUrl = URI(
            (execution.getVariable("portaalTaakObjectUrl")
                ?: throw CompleteTaakProcessVariableNotFoundException("portaalTaakObjectUrl is required but was not provided")) as String
        )

        runWithoutAuthorization { taskService.complete(verwerkerTaakId) }

        logger.info { "Task with id '${verwerkerTaakId}' for object with URL '${portaalTaakObjectUrl}' completed" }

        val portaaltaakObject = objectManagement.getObject(portaalTaakObjectUrl)

        when (version) {
            TaakVersion.V1 -> completeTaakV1(portaaltaakObject, objectManagement, portaalTaakObjectUrl)
            TaakVersion.V2 -> completeTaakV2(objectManagement = objectManagement, portaaltaakObject = portaaltaakObject)
        }
    }

    internal fun completeTaakV1(
        portaaltaakObject: ObjectWrapper,
        objectManagement: ObjectManagement,
        portaalTaakObjectUrl: URI
    ) {
        var taakObject: TaakObject = objectMapper
            .convertValue(
                portaaltaakObject.record.data
                    ?: throw RuntimeException("Portaaltaak meta data was empty!")
            )
        taakObject = changeStatus(taakObject, VERWERKT)
        val portaalTaakMetaObjectUpdated =
            changeDataInPortalTaakObject(portaaltaakObject, objectMapper.convertValue(taakObject))

        objectManagement.patchObject(portaalTaakObjectUrl, objectMapper.convertValue(portaalTaakMetaObjectUpdated))
            .also {
                logger.info { "Portaaltaak object with URL '${it.url}' completed by changing status to '$VERWERKT'" }
            }
    }

    internal fun completeTaakV2(
        objectManagement: ObjectManagement,
        portaaltaakObject: ObjectWrapper,
    ) {
        val taakObject: TaakObjectV2 =
            objectMapper.convertValue(
                portaaltaakObject.record.data
                    ?: throw RuntimeException("Portaaltaak meta data was empty!")
            )
        val updatedTaakObject = taakObject.copy(status = TaakObjectV2.TaakStatus.VERWERKT)
        objectManagement.patchObject(portaaltaakObject.url, objectMapper.convertValue(updatedTaakObject))
    }

    private fun createTaskV1(
        objectManagement: ObjectManagement,
        config: CreateTaakV1ActionConfig,
        delegateTask: DelegateTask,
    ) {
        logger.debug { "Creating portaaltaak V1 for task with id '${delegateTask.id}'" }

        val processInstanceId = CamundaProcessInstanceId(delegateTask.processInstanceId)
        val documentId = processDocumentService.getDocumentId(processInstanceId, delegateTask).id

        val zaakUrl = try {
            zaakInstanceLinkService.getByDocumentId(documentId).zaakInstanceUrl
        } catch (e: ZaakInstanceLinkNotFoundException) {
            // this should set zaakUrl to null when no zaak has been linked for this case
            null
        }

        val verloopdatum =
            config.verloopDurationInDays?.let { LocalDateTime.now().plusDays(config.verloopDurationInDays) }
                ?: delegateTask.dueDate?.let {
                    LocalDateTime.ofInstant(
                        delegateTask.dueDate.toInstant(),
                        ZoneId.systemDefault()
                    )
                }

        val portaalTaak = TaakObject(
            getTaakIdentification(delegateTask, config.receiver, config.identificationKey, config.identificationValue),
            getTaakData(delegateTask, config.sendData, documentId.toString()),
            delegateTask.name,
            OPEN,
            getTaakForm(config.formType, config.formTypeId, config.formTypeUrl),
            delegateTask.id,
            zaakUrl,
            verloopdatum
        )

        val portalTaskObject = objectManagement.createObject(objectMapper.convertValue(portaalTaak))

        logger.info { "Portaaltaak object with UUID '${portalTaskObject.uuid}' and URL '${portalTaskObject.url}' created for task with id '${delegateTask.id}'" }
    }

    private fun createTaskV2(
        objectManagement: ObjectManagement,
        config: CreateTaakV2ActionConfig,
        delegateTask: DelegateTask
    ) {
        logger.debug { "Creating portaaltaak V2 for task with id '${delegateTask.id}'" }
        val processInstanceId = CamundaProcessInstanceId(delegateTask.processInstanceId)
        val documentId = processDocumentService.getDocumentId(processInstanceId, delegateTask).id
        val verloopdatum: LocalDate? =
            config
                .verloopdatum
                ?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (ex: DateTimeParseException) {
                        logger.debug {
                            "Failed to parse $it as LocalDate. Check your plugin action configuration."
                        }
                        throw ex
                    }
                }
                ?: delegateTask.dueDate?.let {
                    LocalDate.ofInstant(
                        it.toInstant(),
                        ZoneId.systemDefault()
                    )
                }
        val ogoneBedrag: Double? =
            config.ogoneBedrag
                ?.let {
                    requireNotNull(it.toDoubleOrNull()) {
                        "Failed to parse $it as Double. Check your plugin action configuration."
                    }
                }
        val portaalTaak = TaakObjectV2(
            titel = delegateTask.name,
            status = TaakObjectV2.TaakStatus.OPEN,
            soort = config.taakSoort,
            verloopdatum = verloopdatum,
            identificatie = when (config.receiver) {
                ZAAK_INITIATOR -> {
                    val identification = getTaakIdentification(
                        delegateTask,
                        config.receiver,
                        config.identificationKey,
                        config.identificationValue
                    )
                    TaakObjectV2.TaakIdentificatie(identification.type, identification.value)
                }

                OTHER -> {
                    TaakObjectV2.TaakIdentificatie(
                        type = config.identificationKey!!,
                        value = config.identificationValue!!,
                    )
                }
            },
            koppeling = config.koppelingRegistratie?.let {
                TaakKoppeling(
                    registratie = config.koppelingRegistratie,
                    uuid = config.koppelingUuid
                )
            },
            url = config.taakUrl?.let { TaakUrl(it) },
            portaalformulier = if (config.taakSoort == TaakSoort.PORTAALFORMULIER) {
                PortaalFormulier(
                    type = TaakFormulier(
                        soort = config.portaalformulierSoort!!,
                        value = config.portaalformulierValue!!
                    ),
                    data = getTaakData(delegateTask, config.portaalformulierData, documentId.toString()),
                )
            } else null,
            ogonebetaling = if (config.taakSoort == TaakSoort.OGONEBETALING) {
                OgoneBetaling(
                    bedrag = ogoneBedrag!!,
                    betaalkenmerk = config.ogoneBetaalkenmerk!!,
                    pspid = config.ogonePspid!!
                )
            } else null,
            verwerkerTaakId = delegateTask.id,
            eigenaar = DEFAULT_EIGENAAR
        )

        val portalTaskObject = objectManagement.createObject(objectMapper.convertValue(portaalTaak))

        logger.info { "Portaaltaak object with UUID '${portalTaskObject.uuid}' and URL '${portalTaskObject.url}' created for task with id '${delegateTask.id}'" }
    }

    private inline fun <reified T> resolveActionProperties(config: ObjectNode, execution: DelegateExecution): T {
        val requestedValues = config.properties()
            .filter { it.value.isTextual }
            .mapNotNull { it.value.textValue() }
        val resolvedValues = valueResolverService.resolveValues(
            execution.processInstanceId,
            execution,
            requestedValues
        )

        return objectMapper.convertValue(
            config.properties().associate { (key, value) ->
                key to (resolvedValues.get(value.textValue()) ?: value)
            }
        )
    }

    internal fun getTaakIdentification(
        delegateTask: DelegateTask,
        receiver: TaakReceiver,
        identificationKey: String?,
        identificationValue: String?,
    ): TaakIdentificatie {
        return when (receiver) {
            TaakReceiver.ZAAK_INITIATOR -> getZaakinitiator(delegateTask)
            TaakReceiver.OTHER -> {
                if (identificationKey == null) {
                    throw IllegalStateException("Other was chosen as taak receiver, but no identification key was chosen.")
                }
                if (identificationValue == null) {
                    throw IllegalStateException("Other was chosen as taak receiver, but no identification value was chosen.")
                }
                TaakIdentificatie(
                    identificationKey,
                    identificationValue
                )
            }
        }
    }

    internal fun getZaakinitiator(delegateTask: DelegateTask): TaakIdentificatie {
        val processInstanceId = CamundaProcessInstanceId(delegateTask.processInstanceId)
        val documentId = processDocumentService.getDocumentId(processInstanceId, delegateTask)

        val zaakUrl = zaakInstanceLinkService.getByDocumentId(documentId.id).zaakInstanceUrl
        val zakenPlugin = requireNotNull(
            pluginService.createInstance(ZakenApiPlugin::class.java, ZakenApiPlugin.findConfigurationByUrl(zaakUrl))
        ) { "No plugin configuration was found for zaak with URL $zaakUrl" }

        val initiator = requireNotNull(
            zakenPlugin.getZaakRollen(zaakUrl, RolType.INITIATOR).firstOrNull()
        ) { "No initiator role found for zaak with URL $zaakUrl" }

        return requireNotNull(
            initiator.betrokkeneIdentificatie.let {
                when (it) {
                    is RolNatuurlijkPersoon -> TaakIdentificatie(
                        TaakIdentificatie.TYPE_BSN,
                        requireNotNull(it.inpBsn) {
                            "Zaak initiator did not have valid inpBsn BSN"
                        }
                    )

                    is RolNietNatuurlijkPersoon -> TaakIdentificatie(
                        TaakIdentificatie.TYPE_KVK,
                        requireNotNull(it.annIdentificatie) {
                            "Zaak initiator did not have valid annIdentificatie KVK"
                        }
                    )

                    else -> null
                }
            }
        ) { "Could not map initiator identificatie (value=${initiator.betrokkeneIdentificatie}) for zaak with URL $zaakUrl to TaakIdentificatie" }
    }

    internal fun getTaakForm(
        formType: TaakFormType,
        formTypeId: String?,
        formTypeUrl: String?
    ): TaakForm {
        return TaakForm(
            formType,
            when (formType) {
                ID -> formTypeId
                    ?: throw IllegalStateException("formTypeId can not be null when formType ID has been chosen")

                URL -> formTypeUrl
                    ?: throw IllegalStateException("formTypeUrl can not be null when formType URL has been chosen")
            }
        )
    }

    internal fun getTaakData(
        delegateTask: DelegateTask,
        sendData: List<DataBindingConfig>,
        documentId: String
    ): Map<String, Any> {
        val sendDataValuesResolvedMap = valueResolverService.resolveValues(documentId, sendData.map { it.value })

        if (sendData.size != sendDataValuesResolvedMap.size) {
            val failedValues = sendData
                .filter { !sendDataValuesResolvedMap.containsKey(it.value) }
                .joinToString(", ") { "'${it.key}' = '${it.value}'" }
            throw IllegalArgumentException(
                "Error in sendData for task: '${delegateTask.taskDefinitionKey}' and documentId: '${documentId}'. Failed to resolve values: $failedValues".trimMargin()
            )
        }

        val sendDataResolvedMap = sendData.associate { it.key to sendDataValuesResolvedMap[it.value] }
        val jsonPatchBuilder = JsonPatchBuilder()
        val taakData = objectMapper.createObjectNode()

        sendDataResolvedMap.forEach {
            val path = JsonPointer.valueOf(it.key)
            val valueNode = objectMapper.valueToTree<JsonNode>(it.value)
            jsonPatchBuilder.addJsonNodeValue(taakData, path, valueNode)
        }

        JsonPatchService.apply(jsonPatchBuilder.build(), taakData)

        return objectMapper.convertValue(taakData)
    }

    internal fun changeStatus(taakObject: TaakObject, status: TaakStatus): TaakObject {
        return TaakObject(
            taakObject.identificatie,
            taakObject.data,
            taakObject.title,
            status,
            taakObject.formulier,
            taakObject.verwerkerTaakId,
            taakObject.zaakUrl,
            taakObject.verloopdatum,
            taakObject.verzondenData,
        )
    }

    internal fun changeDataInPortalTaakObject(
        portaalTaakMetaObject: ObjectWrapper,
        convertValue: JsonNode
    ): ObjectRequest {
        return ObjectRequest(
            type = portaalTaakMetaObject.type,
            record = ObjectRecord(
                data = convertValue,
                correctedBy = portaalTaakMetaObject.record.correctedBy,
                endAt = portaalTaakMetaObject.record.endAt,
                index = portaalTaakMetaObject.record.index,
                geometry = portaalTaakMetaObject.record.geometry,
                registrationAt = portaalTaakMetaObject.record.registrationAt,
                startAt = portaalTaakMetaObject.record.startAt,
                typeVersion = portaalTaakMetaObject.record.typeVersion
            )
        )
    }

    private fun ObjectManagement.getObject(
        objectUrl: URI,
    ): ObjectWrapper {
        val objectenApiPlugin: ObjectenApiPlugin =
            pluginService.createInstance(objectenApiPluginConfigurationId)

        return objectenApiPlugin.getObject(objectUrl)
    }

    private fun ObjectManagement.createObject(
        objectData: JsonNode,
    ): ObjectWrapper {
        val objectenApiPlugin: ObjectenApiPlugin =
            pluginService.createInstance(objectenApiPluginConfigurationId)
        val objecttypenApiPlugin = pluginService
            .createInstance(PluginConfigurationId(objecttypenApiPluginConfigurationId)) as ObjecttypenApiPlugin
        val objectTypeUrl = objecttypenApiPlugin.getObjectTypeUrlById(objecttypeId)
        val createObjectRequest = ObjectRequest(
            objectTypeUrl,
            ObjectRecord(
                typeVersion = objecttypeVersion,
                data = objectData,
                startAt = LocalDate.now()
            )
        )

        return objectenApiPlugin.createObject(createObjectRequest)
    }

    private fun ObjectManagement.patchObject(
        objectUrl: URI,
        objectData: JsonNode,
    ): ObjectWrapper {
        val objectenApiPlugin: ObjectenApiPlugin =
            pluginService.createInstance(objectenApiPluginConfigurationId)
        val objecttypenApiPlugin: ObjecttypenApiPlugin = pluginService
            .createInstance(objecttypenApiPluginConfigurationId)
        val objectTypeUrl = objecttypenApiPlugin.getObjectTypeUrlById(objecttypeId)
        val createObjectRequest = ObjectRequest(
            objectTypeUrl,
            ObjectRecord(
                typeVersion = objecttypeVersion,
                data = objectData,
                startAt = LocalDate.now()
            )
        )

        return objectenApiPlugin.objectPatch(objectUrl, createObjectRequest)
    }

    companion object {
        private const val DEFAULT_EIGENAAR = "GZAC"
        private val logger: KLogger = KotlinLogging.logger {}
        private val objectMapper: ObjectMapper = MapperSingleton.get()
    }
}