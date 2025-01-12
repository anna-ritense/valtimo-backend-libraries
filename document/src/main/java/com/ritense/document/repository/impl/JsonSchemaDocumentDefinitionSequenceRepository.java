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

package com.ritense.document.repository.impl;

import com.ritense.document.domain.impl.sequence.JsonSchemaDocumentDefinitionSequenceRecord;
import com.ritense.document.repository.DocumentDefinitionSequenceRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JsonSchemaDocumentDefinitionSequenceRepository extends DocumentDefinitionSequenceRepository<JsonSchemaDocumentDefinitionSequenceRecord> {

    @Query("" +
        "   SELECT  ddsr " +
        "   FROM    JsonSchemaDocumentDefinitionSequenceRecord ddsr " +
        "   WHERE   ddsr.id.name = :documentDefinitionName ")
    Optional<JsonSchemaDocumentDefinitionSequenceRecord> findByDefinitionName(
        @Param("documentDefinitionName") String documentDefinitionName
    );

    @Modifying
    @Query("" +
        "   DELETE " +
        "   FROM    JsonSchemaDocumentDefinitionSequenceRecord ddsr " +
        "   WHERE   ddsr.id.name = :documentDefinitionName ")
    void deleteByDocumentDefinitionName(
        @Param("documentDefinitionName") String documentDefinitionName
    );

}