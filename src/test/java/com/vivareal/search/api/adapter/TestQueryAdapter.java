package com.vivareal.search.api.adapter;

import com.vivareal.search.api.model.SearchApiRequest;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class TestQueryAdapter extends AbstractQueryAdapter<Void, Void, Void> {

    @Override
    public Object getById(SearchApiRequest request, String id) {
        return null;
    }

    @Override
    public List<Map<String, Object>> getQueryMarcao(SearchApiRequest request) {
        return null;
    }

    @Override
    public void stream(SearchApiRequest request, OutputStream stream) { }

    @Override
    protected Void getFilter(List<String> filter) {
        return null;
    }

    @Override
    protected Void getSort(List<String> sort) {
        return null;
    }

}
