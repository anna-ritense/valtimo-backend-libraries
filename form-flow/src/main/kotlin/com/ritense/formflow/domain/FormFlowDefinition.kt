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

package com.ritense.formflow.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Objects
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "form_flow_definition")
class FormFlowDefinition(
    @JsonProperty("key")
    key: String = "",

    @EmbeddedId
    @JsonIgnore
    val id: FormFlowDefinitionId = FormFlowDefinitionId.newId(key),

    @Column(name = "start_step")
    val startStep: String,

    @OneToMany(mappedBy = "id.formFlowDefinition", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val steps: Set<FormFlowStep>,
) {
    init {
        steps.forEach { step -> step.id.formFlowDefinition = this }
    }

    override fun hashCode(): Int {
        return Objects.hash(id, startStep, steps)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FormFlowDefinition

        if (id != other.id) return false
        if (startStep != other.startStep) return false
        if (steps != other.steps) return false

        return true
    }

}