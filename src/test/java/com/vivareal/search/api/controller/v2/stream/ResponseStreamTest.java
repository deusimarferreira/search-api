package com.vivareal.search.api.controller.v2.stream;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResponseStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenStreamIsNull() {
        ResponseStream.create(null);
    }

    @Test
    public void shouldCallWriteAndFlush() throws IOException {
        OutputStream mockStream = mock(OutputStream.class);

        ResponseStream.create(mockStream).write(new byte[0]);

        verify(mockStream).write(any(byte[].class));
        verify(mockStream).flush();
    }

    @Test
    public void shouldCreateFlatedByteArray() {
        OutputStream mockStream = mock(OutputStream.class);

        final class ByteArray {
            private byte[] array;

            private ByteArray(byte[] array) {
                this.array = array;
            }

            private byte[] getArray() {
                return array;
            }
        }

        ByteArray data[] = {
                new ByteArray(new byte[]{1, 2, 3}),
                new ByteArray(new byte[]{4, 5, 6, 7}),
                new ByteArray(new byte[]{8, 9, 10, 11, 12})
        };

        byte[] result = ResponseStream.create(mockStream).flatArray(data, ByteArray::getArray);

        assertEquals(12, result.length);

        assertTrue(Arrays.equals(data[0].array, new byte[]{result[0], result[1], result[2]}));
        assertTrue(Arrays.equals(data[1].array, new byte[]{result[3], result[4], result[5], result[6]}));
        assertTrue(Arrays.equals(data[2].array, new byte[]{result[7], result[8], result[9], result[10], result[11]}));
    }
}