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

package com.ritense.mail.wordpressmail

import com.nhaarman.mockitokotlin2.whenever
import com.ritense.connector.service.ConnectorService
import com.ritense.mail.wordpressmail.connector.WordpressMailConnector
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(value = [SpringExtension::class])
@Tag("integration")
abstract class BaseIntegrationTest {

    @MockBean
    lateinit var connectorService: ConnectorService

    @Mock
    lateinit var wordpressMailConnector: WordpressMailConnector

    @BeforeEach
    fun beforeEach() {
        whenever(connectorService.loadByClassName(WordpressMailConnector::class.java))
            .thenReturn(wordpressMailConnector)
    }
}