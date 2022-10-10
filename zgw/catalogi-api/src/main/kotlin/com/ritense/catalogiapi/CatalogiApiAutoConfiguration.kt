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

package com.ritense.catalogiapi

import com.ritense.catalogiapi.service.ZaaktypeUrlProvider
import com.ritense.objectenapi.security.CatalogiApiHttpSecurityConfigurer
import com.ritense.objectenapi.service.CatalogiService
import com.ritense.plugin.service.PluginService
import com.ritense.zakenapi.client.CatalogiApiClient
import io.netty.handler.logging.LogLevel
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat

@Configuration
class CatalogiApiAutoConfiguration {

    @Bean
    fun catalogiApiClient(webclient: WebClient): CatalogiApiClient {
        return CatalogiApiClient(webclient)
    }

    @Bean
    fun catalogiApiPluginFactory(
        pluginService: PluginService,
        client: CatalogiApiClient
    ): CatalogiApiPluginFactory {
        return CatalogiApiPluginFactory(pluginService, client)
    }

    @Bean
    @ConditionalOnMissingBean(WebClient::class)
    fun catalogiApiWebClient(): WebClient {
        return WebClient.builder().clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create().wiretap(
                    "reactor.netty.http.client.HttpClient",
                    LogLevel.DEBUG,
                    AdvancedByteBufFormat.TEXTUAL
                )
            )
        ).build()
    }

    @Bean
    fun catalogiService(
        zaaktypeUrlProvider: ZaaktypeUrlProvider,
        pluginService : PluginService
    ): CatalogiService {
        return CatalogiService(zaaktypeUrlProvider, pluginService)
    }

    @Order(400)
    @Bean
    fun catalogiApiHttpSecurityConfigurer(): CatalogiApiHttpSecurityConfigurer {
        return CatalogiApiHttpSecurityConfigurer()
    }
}