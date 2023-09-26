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

package com.ritense.document.domain.impl.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class AssignToDocumentsRequest {
    @JsonProperty
    @NotNull
    private final List<UUID> documentIds;

    @JsonProperty
    @NotNull
    private final String assigneeId;

    @JsonCreator
    public AssignToDocumentsRequest(@JsonProperty(value = "documentIds", required = true) List<UUID> documentIds,
                                    @JsonProperty(value = "assigneeId", required = true) String assigneeId
    ) {
        this.documentIds = documentIds;
        this.assigneeId = assigneeId;
    }

    public List<UUID> getDocumentIds() {
        return documentIds;
    }

    public String getAssigneeId() {
        return assigneeId;
    }
}