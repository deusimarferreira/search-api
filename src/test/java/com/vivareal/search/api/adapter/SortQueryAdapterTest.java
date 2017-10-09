package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.http.SearchApiRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.ES_DEFAULT_SORT;
import static com.vivareal.search.api.model.http.SearchApiRequestBuilder.INDEX_NAME;
import static com.vivareal.search.api.model.mapping.MappingType.FIELD_TYPE_NESTED;
import static org.assertj.core.util.Sets.newLinkedHashSet;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class SortQueryAdapterTest extends SearchTransportClientMock {

    private SettingsAdapter<Map<String, Map<String, Object>>, String> settingsAdapter = mock(SettingsAdapter.class);

    private SortQueryAdapter sortQueryAdapter = new SortQueryAdapter(settingsAdapter);

    @Test
    public void shouldApplySortByDefault() {
        ES_DEFAULT_SORT.setValue(INDEX_NAME, "id ASC");

        SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
        SearchApiRequest request = fullRequest.build();

        when(settingsAdapter.isTypeOf(request.getIndex(), "id", FIELD_TYPE_NESTED)).thenReturn(false);

        sortQueryAdapter.apply(requestBuilder, request);
        List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

        assertEquals("id", sorts.get(0).getFieldName());
        assertEquals("ASC", sorts.get(0).order().name());
        assertNull(sorts.get(0).getNestedPath());
    }

    @Test
    public void shouldApplySortByRequest() {
        String fieldName1 = "field";
        SortOrder sortOrder1 = ASC;

        String fieldName2 = "nested.field";
        SortOrder sortOrder2 = DESC;

        SearchRequestBuilder requestBuilder = transportClient.prepareSearch(INDEX_NAME);
        SearchApiRequest request = fullRequest.build();
        request.setSort(fieldName1 + " " + sortOrder1.name() + ", " + fieldName2 + " " + sortOrder2.name());

        when(settingsAdapter.isTypeOf(request.getIndex(), fieldName1, FIELD_TYPE_NESTED)).thenReturn(false);
        when(settingsAdapter.isTypeOf(request.getIndex(), fieldName2.split("\\.")[0], FIELD_TYPE_NESTED)).thenReturn(true);

        sortQueryAdapter.apply(requestBuilder, request);
        List<FieldSortBuilder> sorts = (List) requestBuilder.request().source().sorts();

        assertEquals(fieldName1, sorts.get(0).getFieldName());
        assertEquals(sortOrder1, sorts.get(0).order());
        assertNull(sorts.get(0).getNestedPath());

        assertEquals(fieldName2, sorts.get(1).getFieldName());
        assertEquals(sortOrder2, sorts.get(1).order());
        assertEquals("nested", sorts.get(1).getNestedPath());
    }

}
