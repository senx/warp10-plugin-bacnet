package fr.couincouin;


import java.io.IOException;
import java.io.InputStream;

import com.serotonin.bacnet4j.util.sero.ThreadUtils;

import jssc.SerialPort;
import jssc.SerialPortException;

public class JsscSerialPortInputStream extends InputStream {
  private final SerialPort serialPort;

  public JsscSerialPortInputStream(final SerialPort serialPort) {
    this.serialPort = serialPort;
  }

  @Override
  public int read() throws IOException {
    try {
      while (true) {
        final byte[] b = serialPort.readBytes(1);
        if (b == null) {
          ThreadUtils.sleep(20);
          continue;
        }
        return b[0];
      }
    } catch (final SerialPortException e) {
      throw new IOException(e);
    }
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (len == 0) {
      return 0;
    }

    try {
      int length = serialPort.getInputBufferBytesCount();
      if (length > len) {
        length = len;
      }

      final byte[] buf = serialPort.readBytes(length);
      System.arraycopy(buf, 0, b, off, length);
      return length;
    } catch (final SerialPortException e) {
      throw new IOException(e);
    }
  }

  @Override
  public int available() throws IOException {
    try {
      return serialPort.getInputBufferBytesCount();
    } catch (final SerialPortException e) {
      throw new IOException(e);
    }
  }

  //    @Override
  //    public void close() throws IOException {
  //        // TODO Auto-generated method stub
  //        super.close();
  //    }
}
