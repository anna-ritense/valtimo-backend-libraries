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

package com.ritense.document.service.impl;

import com.ritense.document.BaseIntegrationTest;
import com.ritense.document.domain.impl.searchfield.SearchFieldDatatype;
import com.ritense.document.domain.impl.searchfield.SearchFieldFieldtype;
import com.ritense.document.domain.impl.searchfield.SearchFieldMatchtype;
import com.ritense.document.service.SearchFieldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SearchConfigurationDeploymentServiceIntTest extends BaseIntegrationTest {

    @Autowired
    public SearchFieldService searchFieldService;

    @Test
    void shouldDeploySearchConfigurationFromResourceFolder() {
        var documentDefinitionName = "profile";

        var searchFields = searchFieldService.getSearchFields(documentDefinitionName);

        assertThat(searchFields).hasSize(1);
        assertThat(searchFields.get(0).getId().getDocumentDefinitionName()).isEqualTo("profile");
        assertThat(searchFields.get(0).getKey()).isEqualTo("lastname");
        assertThat(searchFields.get(0).getPath()).isEqualTo("/lastname");
        assertThat(searchFields.get(0).getDatatype()).isEqualTo(SearchFieldDatatype.TEXT);
        assertThat(searchFields.get(0).getFieldtype()).isEqualTo(SearchFieldFieldtype.SINGLE);
        assertThat(searchFields.get(0).getMatchtype()).isEqualTo(SearchFieldMatchtype.LIKE);
    }

}