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

import com.ritense.portaaltaak.domain.TaakObjectV2.FormulierSoort
import com.ritense.portaaltaak.domain.TaakObjectV2.TaakKoppelingRegistratie
import com.ritense.portaaltaak.domain.TaakObjectV2.TaakSoort

data class CreateTaakV2ActionConfig(
    val taakSoort: TaakSoort,
    val taakUrl: String?,
    val portaalformulierSoort: FormulierSoort?,
    val portaalformulierValue: String?,
    val portaalformulierData: List<DataBindingConfig> = emptyList(),
    val portaalformulierVerzondenData: List<DataBindingConfig> = emptyList(),
    val ogoneBedrag: String?,
    val ogoneBetaalkenmerk: String?,
    val ogonePspid: String?,
    val receiver: TaakReceiver,
    val identificationKey: String?,
    val identificationValue: String?,
    val verloopdatum: String?,
    val koppelingRegistratie: TaakKoppelingRegistratie?,
    val koppelingUuid: String?,
)