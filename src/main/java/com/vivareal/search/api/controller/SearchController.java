package com.vivareal.search.api.controller;

import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.*;
import static com.vivareal.search.api.configuration.ThreadPoolConfig.MAX_SIZE;
import static com.vivareal.search.api.configuration.ThreadPoolConfig.MIN_SIZE;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.newrelic.api.agent.Trace;
import com.vivareal.search.api.controller.error.ExceptionHandler;
import com.vivareal.search.api.model.http.BaseApiRequest;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import com.vivareal.search.api.model.http.SearchApiRequest;
import com.vivareal.search.api.model.serializer.SearchResponseEnvelope;
import com.vivareal.search.api.service.SearchService;
import com.vivareal.search.api.service.parser.IndexSettings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.elasticsearch.action.get.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/v2")
@Api("v2")
@DefaultProperties(
  defaultFallback = "fallback",
  ignoreExceptions = {IllegalArgumentException.class}
)
public class SearchController {

  private static final BodyBuilder builderOK = ok();

  private static final ResponseEntity<Object> notFoundResponse = notFound().build();

  @Autowired private SearchService searchService;

  @Autowired private IndexSettings indexSettings;

  @Autowired private Environment environment;

  @Autowired private ExceptionHandler exceptionHandler;

  @RequestMapping(
    value = {"/{index}/{id:[0-9]+}"},
    method = GET,
    produces = MediaType.APPLICATION_JSON_UTF8_VALUE
  )
  @ApiOperation(value = "Search by index with id", notes = "Returns index by identifier")
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "Successfully get by id"),
      @ApiResponse(code = 400, message = "Bad parameters request"),
      @ApiResponse(code = 404, message = "Id not found on cluster"),
      @ApiResponse(code = 500, message = "Internal Server Error")
    }
  )
  @HystrixCommand(
    commandProperties = {
      @HystrixProperty(name = EXECUTION_ISOLATION_STRATEGY, value = "SEMAPHORE"),
      @HystrixProperty(
        name = EXECUTION_ISOLATION_SEMAPHORE_MAX_CONCURRENT_REQUESTS,
        value = "1024"
      ),
      @HystrixProperty(name = EXECUTION_TIMEOUT_ENABLED, value = "false"),
      @HystrixProperty(name = CIRCUIT_BREAKER_SLEEP_WINDOW_IN_MILLISECONDS, value = "5000"),
      @HystrixProperty(name = CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD, value = "100"),
      @HystrixProperty(name = CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE, value = "90")
    },
    threadPoolProperties = {
      @HystrixProperty(name = CORE_SIZE, value = MIN_SIZE),
      @HystrixProperty(name = MAXIMUM_SIZE, value = MAX_SIZE)
    }
  )
  @Trace(dispatcher = true)
  public ResponseEntity<Object> id(BaseApiRequest request, @PathVariable String id) {
    indexSettings.validateIndex(request);
    GetResponse response = searchService.getById(request, id);

    if (!response.isExists()) return notFoundResponse;

    return builderOK.body(new String(response.getSourceAsBytes()));
  }

  @RequestMapping(
    value = "/{index}",
    method = GET,
    produces = MediaType.APPLICATION_JSON_UTF8_VALUE
  )
  @ApiOperation(value = "Search documents", notes = "Returns query-based documents")
  @ApiResponses(
    value = {
      @ApiResponse(code = 200, message = "Successfully get documents"),
      @ApiResponse(code = 400, message = "Bad parameters request"),
      @ApiResponse(code = 500, message = "Internal Server Error")
    }
  )
  @HystrixCommand(
    commandProperties = {
      @HystrixProperty(name = EXECUTION_ISOLATION_STRATEGY, value = "SEMAPHORE"),
      @HystrixProperty(
        name = EXECUTION_ISOLATION_SEMAPHORE_MAX_CONCURRENT_REQUESTS,
        value = "1024"
      ),
      @HystrixProperty(name = EXECUTION_TIMEOUT_ENABLED, value = "false"),
      @HystrixProperty(name = CIRCUIT_BREAKER_SLEEP_WINDOW_IN_MILLISECONDS, value = "10000"),
      @HystrixProperty(name = CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD, value = "30"),
      @HystrixProperty(name = CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE, value = "70")
    },
    threadPoolProperties = {
      @HystrixProperty(name = CORE_SIZE, value = MIN_SIZE),
      @HystrixProperty(name = MAXIMUM_SIZE, value = MAX_SIZE)
    }
  )
  @Trace(dispatcher = true)
  public ResponseEntity<Object> search(SearchApiRequest request) {
    indexSettings.validateIndex(request);
    return builderOK.body(
        new SearchResponseEnvelope<>(request.getIndex(), searchService.search(request)));
  }

  public ResponseEntity<Object> fallback(Throwable e) {
    ResponseEntity<Map<String, Object>> error = exceptionHandler.error(e);
    return new ResponseEntity<>(error.getBody(), error.getStatusCode());
  }

  @RequestMapping(
    value = {"/force{operation:Open|Closed}/{flag}"},
    method = GET
  )
  @ApiIgnore
  public ResponseEntity<Object> forceOpen(
      @PathVariable String operation, @PathVariable boolean flag) {
    if (Arrays.stream(environment.getActiveProfiles())
        .noneMatch(env -> env.equalsIgnoreCase("test"))) return notFoundResponse;

    ConfigurationManager.getConfigInstance()
        .setProperty("hystrix.command.default.circuitBreaker.force" + operation, flag);
    return builderOK.build();
  }

  @RequestMapping(value = "/{index}/stream", method = GET)
  @ApiIgnore
  @Trace(dispatcher = true)
  public StreamingResponseBody stream(
      FilterableApiRequest request, HttpServletResponse httpServletResponse) {
    indexSettings.validateIndex(request);
    httpServletResponse.setContentType("application/x-ndjson;charset=UTF-8");
    return out -> searchService.stream(request, out);
  }
}
