package com.vivareal.search.api.controller.stream;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import org.junit.Test;

public class ResponseStreamTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenStreamIsNull() {
    ResponseStream.iterate(null, null, null);
  }

  @Test
  public void shouldSuccessfulIterate() throws IOException {
    OutputStream mockStream = mock(OutputStream.class);

    final String[][] data = {
      {"tincas", "urubu", "pizza"},
      {"socks", "pinto"},
      {}
    };

    ResponseStream.iterate(
        mockStream,
        new Iterator<String[]>() {
          private int counter = 0;

          @Override
          public boolean hasNext() {
            return counter < data.length;
          }

          @Override
          public String[] next() {
            return data[counter++];
          }
        },
        String::getBytes);

    int lineEndCallCount = 0;
    int flushCallCount = 0;
    for (String[] hits : data) {
      for (String hit : hits) {
        verify(mockStream, times(1)).write(hit.getBytes());
        ++lineEndCallCount;
      }
      ++flushCallCount;
    }
    ++lineEndCallCount;
    ++flushCallCount;

    verify(mockStream, times(lineEndCallCount)).write('\n');
    verify(mockStream, times(flushCallCount)).flush();
  }
}
