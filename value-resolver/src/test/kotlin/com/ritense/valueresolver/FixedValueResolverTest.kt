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

package com.ritense.valueresolver

import java.util.UUID
import org.assertj.core.api.Assertions
import org.camunda.bpm.extension.mockito.delegate.DelegateTaskFake
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FixedValueResolverTest {

    private val fixedValueResolver = FixedValueResolverFactory()

    @Test
    fun `should resolve boolean value from requestedValue`() {
        val processInstanceId = UUID.randomUUID().toString()
        val variableScope = DelegateTaskFake()

        val resolvedValue = fixedValueResolver.createResolver(
            processInstanceId = processInstanceId,
            variableScope = variableScope
        ).apply(
            "true"
        )

        Assertions.assertThat(resolvedValue).isEqualTo(true)
    }

    @Test
    fun `should resolve long value from requestedValue`() {
        val processInstanceId = UUID.randomUUID().toString()
        val variableScope = DelegateTaskFake()

        val resolvedValue = fixedValueResolver.createResolver(
            processInstanceId = processInstanceId,
            variableScope = variableScope
        ).apply(
            "1337"
        )

        Assertions.assertThat(resolvedValue).isEqualTo(1337L)
    }

    @Test
    fun `should resolve double value from requestedValue`() {
        val processInstanceId = UUID.randomUUID().toString()
        val variableScope = DelegateTaskFake()

        val resolvedValue = fixedValueResolver.createResolver(
            processInstanceId = processInstanceId,
            variableScope = variableScope
        ).apply(
            "13.37"
        )

        Assertions.assertThat(resolvedValue).isEqualTo(13.37)
    }

    @Test
    fun `should resolve string value from requestedValue`() {
        val processInstanceId = UUID.randomUUID().toString()
        val variableScope = DelegateTaskFake()

        val resolvedValue = fixedValueResolver.createResolver(
            processInstanceId = processInstanceId,
            variableScope = variableScope
        ).apply(
            "asdf"
        )

        Assertions.assertThat(resolvedValue).isEqualTo("asdf")
    }

    @Test
    fun `should NOT handle value`() {
        val processInstanceId = UUID.randomUUID().toString()
        val variableScope = DelegateTaskFake()

        assertThrows<RuntimeException>("Can't handle value that doesn't have a prefix. firstName to John") {
            fixedValueResolver.handleValues(processInstanceId, variableScope, mapOf("firstName" to "John"))
        }
    }

}