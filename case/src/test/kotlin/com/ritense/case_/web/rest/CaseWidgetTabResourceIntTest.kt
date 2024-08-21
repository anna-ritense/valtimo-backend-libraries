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

package com.ritense.case_.web.rest

import com.ritense.authorization.AuthorizationContext.Companion.runWithoutAuthorization
import com.ritense.case.BaseIntegrationTest
import com.ritense.case.domain.CaseTabType
import com.ritense.case.service.CaseTabService
import com.ritense.case.web.rest.dto.CaseTabDto
import com.ritense.case_.rest.dto.CaseWidgetTabDto
import com.ritense.case_.service.CaseWidgetTabService
import com.ritense.case_.web.rest.dto.TestCaseWidgetTabWidgetDto
import com.ritense.case_.widget.TestCaseWidgetProperties
import com.ritense.case_.widget.displayproperties.CurrencyFieldDisplayProperties
import com.ritense.document.domain.impl.request.NewDocumentRequest
import com.ritense.document.service.impl.JsonSchemaDocumentService
import com.ritense.valtimo.contract.authentication.AuthoritiesConstants.DEVELOPER
import com.ritense.valtimo.contract.authentication.AuthoritiesConstants.USER
import com.ritense.valtimo.contract.json.MapperSingleton
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@Transactional
class CaseWidgetTabResourceIntTest @Autowired constructor(
    private val webApplicationContext: WebApplicationContext,
    private val tabService: CaseTabService,
    private val widgetTabService: CaseWidgetTabService,
    private val documentService: JsonSchemaDocumentService
) : BaseIntegrationTest() {

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    @WithMockUser(username = "user@ritense.com", authorities = [USER])
    fun `should not find case widget tab`() {
        val documentId = UUID.randomUUID().toString()
        val tabKey = "fake-tab"
        mockMvc.perform(
            get("/api/v1/document/{documentId}/widget-tab/{tabKey}", documentId, tabKey)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "developer@ritense.com", authorities = [DEVELOPER])
    fun `should deny access to widget tab when not authorized`() {
        val caseDefinitionName = "some-case-type"
        val tabKey = "my-tab"
        val widgetKey = "my-widget"
        val documentId = runWithoutAuthorization {
            createCaseWidgetTab(caseDefinitionName, tabKey, widgetKey)
            documentService.createDocument(NewDocumentRequest(caseDefinitionName, MapperSingleton.get().createObjectNode())).resultingDocument().get().id()
        }
        mockMvc.perform(
            get("/api/v1/document/{documentId}/widget-tab/{tabKey}", documentId, tabKey)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(print())
            .andExpect(status().is5xxServerError)
    }

    @Test
    @WithMockUser(username = "user@ritense.com", authorities = [USER])
    fun `should find case widget tab`() {
        val caseDefinitionName = "some-case-type"
        val tabKey = "my-tab"
        val widgetKey = "my-widget"
        val documentId = runWithoutAuthorization {
            createCaseWidgetTab(caseDefinitionName, tabKey, widgetKey)
            documentService.createDocument(NewDocumentRequest(caseDefinitionName, MapperSingleton.get().createObjectNode())).resultingDocument().get().id()
        }
        mockMvc.perform(
            get("/api/v1/document/{documentId}/widget-tab/{tabKey}", documentId, tabKey)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.caseDefinitionName").value(caseDefinitionName))
            .andExpect(jsonPath("$.key").value(tabKey))
            .andExpect(jsonPath("$.widgets").exists())
            .andExpect(jsonPath("$.widgets[0].type").value("test"))
            .andExpect(jsonPath("$.widgets[0].key").value("my-widget"))
            .andExpect(jsonPath("$.widgets[0].width").value("1"))
            .andExpect(jsonPath("$.widgets[0].highContrast").value(true))
    }

    @Test
    @WithMockUser(username = "developer@ritense.com", authorities = [DEVELOPER])
    fun `should deny access to case widget data when not authorized`() {
        val caseDefinitionName = "some-case-type"
        val tabKey = "my-tab"
        val widgetKey = "my-widget"
        val documentId = runWithoutAuthorization {
            createCaseWidgetTab(caseDefinitionName, tabKey, widgetKey)
            documentService.createDocument(NewDocumentRequest(caseDefinitionName, MapperSingleton.get().createObjectNode())).resultingDocument().get().id()
        }
        mockMvc.perform(
            get("/api/v1/document/{documentId}/widget-tab/{tabKey}/widget/{widgetKey}", documentId, tabKey, widgetKey)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(print())
            .andExpect(status().is5xxServerError)
    }

    @Test
    @WithMockUser(username = "user@ritense.com", authorities = [USER])
    fun `should get case widget data`() {
        val caseDefinitionName = "some-case-type"
        val tabKey = "my-tab"
        val widgetKey = "my-widget"
        val documentId = runWithoutAuthorization {
            createCaseWidgetTab(caseDefinitionName, tabKey, widgetKey)
            documentService.createDocument(NewDocumentRequest(caseDefinitionName, MapperSingleton.get().createObjectNode())).resultingDocument().get().id()
        }
        mockMvc.perform(
            get("/api/v1/document/{documentId}/widget-tab/{tabKey}/widget/{widgetKey}", documentId, tabKey, widgetKey)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.test").value("test123"))
    }

    private fun createCaseWidgetTab(
        caseDefinitionName: String,
        tabKey: String,
        widgetKey: String
    ): CaseWidgetTabDto {
        tabService.createCaseTab(caseDefinitionName, CaseTabDto(key = tabKey, type = CaseTabType.WIDGETS, contentKey = "-"))
        return widgetTabService.updateWidgetTab(
            CaseWidgetTabDto(
                caseDefinitionName, tabKey, widgets = listOf(
                    TestCaseWidgetTabWidgetDto(
                        widgetKey,
                        "My widget",
                        1,
                        true,
                        TestCaseWidgetProperties(
                            displayProperties = CurrencyFieldDisplayProperties(
                                currencyCode = "EUR"
                            )
                        )
                    )
                )
            )
        )
    }
}