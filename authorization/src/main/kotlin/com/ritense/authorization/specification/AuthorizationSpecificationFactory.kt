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

package com.ritense.authorization.specification

import com.ritense.authorization.permission.Permission
import com.ritense.authorization.request.AuthorizationRequest

interface AuthorizationSpecificationFactory<T : Any> {

    fun create(request: AuthorizationRequest<T>, permissions: List<Permission>): AuthorizationSpecification<T>

    // Change this to something more dynamic in the future
    fun canCreate(request: AuthorizationRequest<*>, permissions: List<Permission>): Boolean
}