package com.grupozap.search.api.adapter;

import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.fieldParserFixture;
import static com.grupozap.search.api.fixtures.model.parser.ParserTemplateLoader.queryParserFixture;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import com.grupozap.search.api.listener.ScriptRemotePropertiesListener;
import com.grupozap.search.api.model.http.SearchApiRequest;
import com.grupozap.search.api.model.parser.OperatorParser;
import com.grupozap.search.api.model.parser.SortParser;
import com.grupozap.search.api.model.parser.ValueParser;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.*;
import org.junit.Test;

public class SortQueryAdapterTest extends SearchTransportClientMock {

  private final SortQueryAdapter sortQueryAdapter;

  public SortQueryAdapterTest() {
    SortParser sortParser =
        new SortParser(
            fieldParserFixture(), new OperatorParser(), new ValueParser(), queryParserFixture());
    this.sortQueryAdapter =
        new SortQueryAdapter(
            sortParser,
            mock(FilterQueryAdapter.class),
            mock(ScriptRemotePropertiesListener.class),
            mock(ElasticsearchSettingsAdapter.class));
  }

  @Test
  public void shouldApplySortByRequest() {
    String fieldName1 = "id";
    SortOrder sortOrder1 = ASC;

    String fieldName2 = "nested.field";
    SortOrder sortOrder2 = DESC;

    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    SearchApiRequest request = fullRequest.build();
    request.setSort(
        fieldName1 + " " + sortOrder1.name() + ", " + fieldName2 + " " + sortOrder2.name());

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.sorts();

    assertEquals(fieldName1, sorts.get(0).getFieldName());
    assertEquals(sortOrder1, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedSort());

    assertEquals(fieldName2, sorts.get(1).getFieldName());
    assertEquals(sortOrder2, sorts.get(1).order());
    assertEquals("nested", sorts.get(1).getNestedSort().getPath());
    assertNull(sorts.get(1).getNestedSort().getFilter());
  }

  @Test
  public void shouldApplySortByScore() {
    String fieldName = "_score";

    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    SearchApiRequest request = fullRequest.build();
    request.setSort(fieldName);

    sortQueryAdapter.apply(requestBuilder, request);
    List<SortBuilder> sorts = (List) requestBuilder.sorts();

    assertEquals(SortOrder.DESC, sorts.get(0).order());
    assertEquals(ScoreSortBuilder.class, sorts.get(0).getClass());
  }

  @Test
  public void shouldApplySortFilterWhenExplicit() {
    String fieldName1 = "id";
    SortOrder sortOrder1 = ASC;

    String fieldName2 = "nested.field";
    SortOrder sortOrder2 = DESC;
    String sortFilter2 = "sortFilter: fieldName EQ \"value\"";

    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    SearchApiRequest request = fullRequest.build();
    request.setSort(
        fieldName1
            + " "
            + sortOrder1.name()
            + ", "
            + fieldName2
            + " "
            + sortOrder2.name()
            + " "
            + sortFilter2);

    BoolQueryBuilder boolQueryBuilder = boolQuery();

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sorts = (List) requestBuilder.sorts();

    assertEquals(fieldName1, sorts.get(0).getFieldName());
    assertEquals(sortOrder1, sorts.get(0).order());
    assertNull(sorts.get(0).getNestedSort());

    assertEquals(fieldName2, sorts.get(1).getFieldName());
    assertEquals(sortOrder2, sorts.get(1).order());
    assertEquals("nested", sorts.get(1).getNestedSort().getPath());
    assertEquals(boolQueryBuilder, sorts.get(1).getNestedSort().getFilter());
  }

  @Test
  public void mustNotApplySortWhenClientDisablesSortOnRequest() {
    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    SearchApiRequest request = fullRequest.build();
    request.setDisableSort(true);

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void mustNotApplySortWhenSortDisabledOnProperty() {
    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    SearchApiRequest request = fullRequest.build();

    sortQueryAdapter.apply(requestBuilder, request);
    assertNull(requestBuilder.sorts());
  }

  @Test
  public void validateDistanceSortBuilder() {
    SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
    SearchApiRequest request = fullRequest.build();
    request.setSort("field.geo NEAR [10.0, -20.0]");

    sortQueryAdapter.apply(requestBuilder, request);
    List<FieldSortBuilder> sortFields = (List) requestBuilder.sorts();

    assertEquals(1, sortFields.size());

    assertTrue((SortBuilder) sortFields.get(0) instanceof GeoDistanceSortBuilder);
    assertEquals("ASC", ((SortBuilder) sortFields.get(0)).order().name());
    assertEquals(
        "field.geo", ((GeoDistanceSortBuilder) (SortBuilder) sortFields.get(0)).fieldName());
  }
}