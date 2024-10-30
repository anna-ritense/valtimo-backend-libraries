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

package com.ritense.portaaltaak.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TaakObjectV2(
    val titel: String,
    var status: TaakStatus,
    val soort: TaakSoort,
    val verloopdatum: LocalDate? = null,
    val identificatie: TaakIdentificatie,
    val koppeling: TaakKoppeling? = null,
    val url: TaakUrl? = null,
    val portaalformulier: PortaalFormulier? = null,
    val ogonebetaling: OgoneBetaling? = null,
    @JsonProperty("verwerker_taak_id") val verwerkerTaakId: String,
    val eigenaar: String,
) {
    enum class TaakSoort(
        @JsonValue val value: String,
    ) {
        URL("url"),
        PORTAALFORMULIER("portaalformulier"),
        OGONEBETALING("ogonebetaling"),
        ;

        override fun toString(): String = value
    }

    enum class TaakStatus(@JsonValue val value: String) {
        OPEN("open"),
        AFGEROND("afgerond"),
        VERWERKT("verwerkt"),
        GESLOTEN("gesloten"),
        ;

        override fun toString(): String = value
    }

    enum class FormulierSoort(@JsonValue val value: String) {
        ID("id"),
        URL("url"),
        ;

        override fun toString(): String = value
    }

    enum class TaakKoppelingRegistratie(
        @JsonValue val value: String,
    ) {
        ZAAK("zaak"),
        PRODUCT("product"),
    }

    data class TaakKoppeling(
        val registratie: TaakKoppelingRegistratie,
        val uuid: String?,
    )

    data class OgoneBetaling(
        val bedrag: Double,
        val betaalkenmerk: String,
        val pspid: String,
    )

    data class TaakIdentificatie(
        val type: String,
        val value: String
    )

    data class PortaalFormulier(
        val type: TaakFormulier,
        val data: Map<String, Any>? = emptyMap(),
        @JsonProperty("verzonden_data")
        var verzondenData: Map<String, Any> = emptyMap(),
    )

    data class TaakFormulier(
        val soort: FormulierSoort,
        val value: String
    )
}