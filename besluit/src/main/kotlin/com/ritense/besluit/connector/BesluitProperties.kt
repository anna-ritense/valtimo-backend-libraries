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

package com.ritense.besluit.connector

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.ritense.connector.config.Decryptor
import com.ritense.connector.config.Encryptor
import com.ritense.connector.domain.ConnectorProperties
import com.ritense.openzaak.domain.configuration.Rsin

class BesluitProperties(
    var url: String = "",
    @set:JsonSerialize(using = Encryptor::class)
    @get:JsonDeserialize(using = Decryptor::class)
    var secret: String = "",
    var clientId: String = "",
    var rsin: Rsin = Rsin("")
) : ConnectorProperties