package com.vivareal.search.api.service;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.controller.v2.stream.ElasticSearchStream;
import com.vivareal.search.api.model.SearchApiRequest;
import com.vivareal.search.api.model.SearchApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Map;

@Component
public class ListingService {

    @Autowired
    @Qualifier("ElasticsearchQuery")
    protected QueryAdapter queryAdapter;

    @Autowired
    private ElasticSearchStream elasticSearch;

    public Map<String, Object> getListingById(SearchApiRequest request, String id) {
        return (Map<String, Object>) this.queryAdapter.getById(request, id);
    }

    public SearchApiResponse getListings(SearchApiRequest request) {
        return this.queryAdapter.query(request);
    }

    public void stream(SearchApiRequest request, OutputStream stream) {
        elasticSearch.stream(request, stream);
    }
}
