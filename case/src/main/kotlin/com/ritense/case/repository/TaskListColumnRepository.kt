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

package com.ritense.case.repository

import com.ritense.case.domain.TaskListColumn
import com.ritense.case.domain.TaskListColumnId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskListColumnRepository : JpaRepository<TaskListColumn, TaskListColumnId> {
    fun findByIdCaseDefinitionNameAndIdKey(caseDefinitionName: String, key: String): TaskListColumn?
    fun findByIdCaseDefinitionNameOrderByOrderAsc(caseDefinitionName: String): List<TaskListColumn>

    @Query("SELECT MAX(tlc.order) FROM TaskListColumn tlc WHERE tlc.id.caseDefinitionName = :caseDefinitionName")
    fun findMaxOrderByIdCaseDefinitionName(@Param("caseDefinitionName") caseDefinitionName: String): Int?

    @Modifying
    @Query("UPDATE TaskListColumn tlc SET tlc.order = tlc.order - 1 WHERE tlc.id.caseDefinitionName = :caseDefinitionName AND tlc.order > :order")
    fun decrementOrderDueToColumnDeletion(@Param("caseDefinitionName") caseDefinitionName: String, @Param("order") order: Int)
}
