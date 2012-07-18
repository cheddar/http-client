package com.metamx.http.client.io;

import com.metamx.common.logger.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 */
public class AppendableByteArrayInputStreamTest
{
  private static final Logger log = new Logger(AppendableByteArrayInputStreamTest.class);

  @Test
  public void testSingleByteArray() throws Exception
  {
    byte[][] bytesToWrite = new byte[][]{{0, 1, 2, 3, 4, 5, 6}};

    testAll(bytesToWrite, bytesToWrite[0]);
  }

  @Test
  public void testMultiByteArray() throws Exception
  {
    byte[] expectedBytes = new byte[]{0, 1, 2, 3, 4, 5, 6};

    testAll(new byte[][]{{0, 1, 2, 3}, {4, 5, 6}}, expectedBytes);
    testAll(new byte[][]{{0, 1}, {2, 3}, {4, 5, 6}}, expectedBytes);
    testAll(new byte[][]{{0}, {1}, {2}, {3}, {4}, {5}, {6}}, expectedBytes);
  }

  public void testAll(byte[][] writtenBytes, byte[] expectedBytes) throws Exception
  {
    testFullRead(writtenBytes, expectedBytes);
    testIndividualRead(writtenBytes, expectedBytes);
  }

  public void testIndividualRead(byte[][] writtenBytes, byte[] expectedBytes) throws IOException
  {
    AppendableByteArrayInputStream in = new AppendableByteArrayInputStream();

    for (byte[] writtenByte : writtenBytes) {
      in.add(writtenByte);
    }

    for (int i = 0; i < expectedBytes.length; i++) {
      final int readByte = in.read();
      if (expectedBytes[i] != (byte) readByte) {
        Assert.assertEquals(String.format("%s[%d", Arrays.toString(expectedBytes), i), expectedBytes[i], readByte);
      }
    }
  }

  public void testFullRead(byte[][] writtenBytes, byte[] expectedBytes) throws IOException
  {
    AppendableByteArrayInputStream in = new AppendableByteArrayInputStream();
    byte[] readBytes = new byte[expectedBytes.length];

    for (byte[] writtenByte : writtenBytes) {
      in.add(writtenByte);
    }
    Assert.assertEquals(readBytes.length, in.read(readBytes));
    Assert.assertArrayEquals(expectedBytes, readBytes);
  }

  @Test
  public void testReadsAndWritesInterspersed() throws Exception
  {
    AppendableByteArrayInputStream in = new AppendableByteArrayInputStream();

    in.add(new byte[]{0, 1, 2});

    byte[] readBytes = new byte[3];
    Assert.assertEquals(3, in.read(readBytes));
    Assert.assertArrayEquals(new byte[]{0, 1, 2}, readBytes);

    in.add(new byte[]{3, 4});
    in.add(new byte[]{5, 6, 7});

    readBytes = new byte[5];
    Assert.assertEquals(5, in.read(readBytes));
    Assert.assertArrayEquals(new byte[]{3, 4, 5, 6, 7}, readBytes);
  }

  @Test
  public void testReadLessThanWritten() throws Exception
  {
    AppendableByteArrayInputStream in = new AppendableByteArrayInputStream();

    in.add(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

    byte[] readBytes = new byte[4];

    Assert.assertEquals(4, in.read(readBytes));
    Assert.assertArrayEquals(new byte[]{0, 1, 2, 3}, readBytes);

    Assert.assertEquals(4, in.read(readBytes));
    Assert.assertArrayEquals(new byte[]{4, 5, 6, 7}, readBytes);

    Assert.assertEquals(2, in.read(readBytes, 0, 2));
    Assert.assertArrayEquals(new byte[]{8, 9, 6, 7}, readBytes);
  }

  @Test
  public void testReadLessThanWrittenMultiple() throws Exception
  {
    AppendableByteArrayInputStream in = new AppendableByteArrayInputStream();

    in.add(new byte[]{0, 1, 2});
    in.add(new byte[]{3, 4, 5});
    in.add(new byte[]{6, 7});
    in.add(new byte[]{8, 9});

    byte[] readBytes = new byte[4];

    Assert.assertEquals(4, in.read(readBytes));
    Assert.assertArrayEquals(new byte[]{0, 1, 2, 3}, readBytes);

    Assert.assertEquals(4, in.read(readBytes));
    Assert.assertArrayEquals(new byte[]{4, 5, 6, 7}, readBytes);

    Assert.assertEquals(2, in.read(readBytes, 0, 2));
    Assert.assertArrayEquals(new byte[]{8, 9, 6, 7}, readBytes);
  }

  @Test
  public void testBlockingRead() throws Exception
  {
    final AppendableByteArrayInputStream in = new AppendableByteArrayInputStream();

    in.add(new byte[]{0, 1, 2, 3, 4});

    Assert.assertEquals(5, in.available());

    Future<byte[]> bytesFuture = Executors.newSingleThreadExecutor().submit(
      new Callable<byte[]>()
      {
        @Override
        public byte[] call() throws Exception
        {
          byte[] readBytes = new byte[10];
          in.read(readBytes);
          return readBytes;
        }
      }
    );

    int count = 0;
    while (in.available() != 0) {
      if (count >= 100) {
        Assert.fail("available didn't become 0 fast enough.");
      }
      count++;
      Thread.sleep(10);
    }

    in.add(new byte[]{5, 6, 7, 8, 9, 10});

    count = 0;
    while (in.available() != 1) {
      if (count >= 100) {
        Assert.fail("available didn't become 1 fast enough.");
      }
      count++;
      Thread.sleep(10);
    }

    Assert.assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, bytesFuture.get());
    Assert.assertEquals(10, in.read());
    Assert.assertEquals(0, in.available());
  }
}