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

package com.ritense.audit.repository;

import com.ritense.audit.domain.AuditRecord;
import com.ritense.audit.domain.AuditRecordId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface AuditRecordRepository<T extends AuditRecord>
    extends JpaRepository<T, AuditRecordId> {

    List<AuditRecord> findAuditRecordsByEvent(String className);

    List<AuditRecord> findAuditRecordsByEventAndProperty(
        String className,
        String key,
        Object value
    );

    Page<AuditRecord> findByEventAndDocumentId(
        List<String> eventTypes,
        UUID documentId,
        Pageable pageable
    );

    List<AuditRecord> findByEventAndOccurredBetween(
        String className,
        LocalDateTime from,
        LocalDateTime until,
        Pageable pageable
    );

    /**
     * Find audit records by property.
     *
     * @deprecated Since 12.0.0.
     */
    @Deprecated(since = "Since 12.0.0", forRemoval = true)
    Page<AuditRecord> findAuditRecordsByProperty(String key, Object value, Pageable pageable);

    void deleteAllBefore(LocalDateTime date);

}