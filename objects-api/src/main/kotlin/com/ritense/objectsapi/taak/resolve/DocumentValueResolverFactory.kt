/*
 * Copyright 2015-2022 Ritense BV, the Netherlands.
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

package com.ritense.objectsapi.taak.resolve

import com.fasterxml.jackson.core.JsonPointer
import com.ritense.processdocument.domain.ProcessInstanceId
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.valtimo.contract.json.Mapper
import java.util.function.Function
import org.camunda.bpm.engine.delegate.VariableScope

/**
 * This resolver can resolve requestedValues against Document linked to the process
 *
 * The value of the requestedValue should be in the format doc:/some/json/pointer
 */
class DocumentValueResolverFactory(
    private val processDocumentService: ProcessDocumentService
) : ValueResolverFactory {

    override fun supportedPrefix(): String {
        return "doc"
    }

    override fun createResolver(
        processInstanceId: ProcessInstanceId,
        variableScope: VariableScope
    ): Function<String, Any?> {
        val document = processDocumentService.getDocument(processInstanceId, variableScope)

        return Function { requestedValue ->
            val value = document.content().getValueBy(JsonPointer.valueOf(requestedValue)).orElse(null)
            if (value?.isValueNode == true) {
                Mapper.INSTANCE.get().treeToValue(value, Object::class.java)
            } else {
                null
            }
        }
    }
}