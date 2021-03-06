package com.grupozap.search.api.itest;

import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.SEARCH_API_PROPERTIES_INDEX;
import static com.grupozap.search.api.itest.configuration.es.ESIndexHandler.TEST_DATA_INDEX;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupozap.search.api.itest.configuration.SearchApiIntegrationTestContext;
import com.grupozap.search.api.itest.configuration.data.StandardDatasetAsserts;
import com.grupozap.search.api.itest.configuration.es.ESIndexHandler;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@RunWith(Enclosed.class)
@TestPropertySource({
  "classpath:application.properties",
  "classpath:application-test.properties",
  "classpath:configuration/application-itest.properties"
})
@ContextConfiguration(classes = SearchApiIntegrationTestContext.class)
public class SearchApiIntegrationTest {

  protected static final ObjectMapper mapper = new ObjectMapper();

  @Value("${itest.standard.dataset.size}")
  protected Integer standardDatasetSize;

  @Value("${es.facet.size}")
  protected Integer facetSize;

  @Value("${search.api.base.url}")
  protected String baseUrl;

  @Value("${es.default.size}")
  protected Integer defaultPageSize;

  @Autowired protected ESIndexHandler esIndexHandler;
  @Autowired protected StandardDatasetAsserts asserts;

  @Before
  public void forceCircuitClosed() {
    given()
        .log()
        .all()
        .baseUri(baseUrl)
        .contentType(JSON)
        .expect()
        .statusCode(SC_OK)
        .when()
        .get("/forceClosed/true");
  }

  @After
  public void setupAfter() {
    esIndexHandler.setDefaultProperties();
    esIndexHandler.addStandardProperties();
  }

  @PostConstruct
  public void setupIndexHandler() throws IOException {
    esIndexHandler.truncateIndexData(TEST_DATA_INDEX);
    esIndexHandler.addStandardTestData();

    esIndexHandler.truncateIndexData(SEARCH_API_PROPERTIES_INDEX);
    esIndexHandler.setDefaultProperties();
    esIndexHandler.addStandardProperties();
  }
}
