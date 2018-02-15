package com.vivareal.search.api.controller.stream;

import static com.vivareal.search.api.configuration.environment.RemoteProperties.*;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;

import com.vivareal.search.api.adapter.QueryAdapter;
import com.vivareal.search.api.model.SearchApiIterator;
import com.vivareal.search.api.model.http.FilterableApiRequest;
import java.io.OutputStream;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchStream {

  @Autowired private TransportClient client;

  @Autowired
  @Qualifier("ElasticsearchQuery")
  private QueryAdapter<?, SearchRequestBuilder> queryAdapter;

  public void stream(FilterableApiRequest request, OutputStream stream) {
    String index = request.getIndex();
    int scrollTimeout = ES_SCROLL_TIMEOUT.getValue(index);
    TimeValue keepAlive = new TimeValue(scrollTimeout);

    SearchRequestBuilder requestBuilder = this.queryAdapter.query(request);
    requestBuilder.setScroll(keepAlive).setSize(ES_STREAM_SIZE.getValue(index));

    int count = MAX_VALUE;
    if (request.getSize() != Integer.MAX_VALUE && request.getSize() != 0) {
      count = request.getSize();
      requestBuilder.setSize(min(request.getSize(), ES_STREAM_SIZE.getValue(index)));
      requestBuilder.setTerminateAfter(count);
    }

    TimeValue streamTimeout = ES_CONTROLLER_STREAM_TIMEOUT.getValue(index);
    ResponseStream.iterate(
        stream,
        new SearchApiIterator<>(
            client,
            requestBuilder.get(),
            scroll -> scroll.setScroll(keepAlive).get(streamTimeout),
            count),
        (SearchHit sh) -> BytesReference.toBytes(sh.getSourceRef()));
  }
}
