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

package com.ritense.processlink.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ritense.processlink.domain.CustomProcessLink.Companion.PROCESS_LINK_TYPE_TEST
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode


class CustomProcessLinkCreateRequestDtoTest {

    private val mapper = jacksonObjectMapper().apply {
        this.registerSubtypes(CustomProcessLinkCreateRequestDto::class.java, CustomProcessLinkUpdateRequestDto::class.java)
    }

    @Test
    fun `should deserialise correctly`() {
        val value: CustomProcessLinkCreateRequestDto = mapper.readValue("""
            {
                "processDefinitionId": "process-definition:1",
                "activityId": "serviceTask1",
                "activityType": "${ActivityTypeWithEventName.SERVICE_TASK_START.value}",
                "processLinkType": "$PROCESS_LINK_TYPE_TEST"
            }
        """.trimIndent())

        assertThat(value, instanceOf(CustomProcessLinkCreateRequestDto::class.java))
        assertThat(value.processLinkType, equalTo(PROCESS_LINK_TYPE_TEST))
    }

    @Test
    fun `should serialize correctly`() {
        val value = CustomProcessLinkCreateRequestDto("process-definition:1","serviceTask1", ActivityTypeWithEventName.SERVICE_TASK_START)

        val json = mapper.writeValueAsString(value)

        JSONAssert.assertEquals("""
            {
              "processDefinitionId": "${value.processDefinitionId}",
              "activityId": "${value.activityId}",
              "activityType": "${value.activityType.value}",
              "processLinkType":"$PROCESS_LINK_TYPE_TEST"
            }
        """.trimIndent(), json, JSONCompareMode.NON_EXTENSIBLE)
    }
}