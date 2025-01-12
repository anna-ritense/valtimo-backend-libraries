/*
 * Copyright 2020 Dimpact.
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

package com.ritense.openzaak.listener

import com.ritense.document.domain.event.DocumentCreatedEvent
import com.ritense.openzaak.service.impl.ZaakService
import com.ritense.openzaak.service.ZaakTypeLinkService
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order

@Deprecated("Since 12.0.0")
class DocumentCreatedListener(
    val zaakService: ZaakService,
    val zaakTypeLinkService: ZaakTypeLinkService
) {
    @Order(0)
    @EventListener(DocumentCreatedEvent::class)
    fun handle(event: DocumentCreatedEvent) {
        val zaakTypeLink = zaakTypeLinkService.get(event.definitionId().name())
        zaakTypeLink?.let {
            if (it.createWithDossier && it.zakenApiPluginConfigurationId == null) {
                zaakService.createZaakWithLink(event.documentId())
            }
        }
    }
}