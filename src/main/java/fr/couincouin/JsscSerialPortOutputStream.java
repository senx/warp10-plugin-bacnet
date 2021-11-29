package fr.couincouin;


import java.io.IOException;
import java.io.OutputStream;

import jssc.SerialPort;
import jssc.SerialPortException;

public class JsscSerialPortOutputStream extends OutputStream {
  private final SerialPort serialPort;

  public JsscSerialPortOutputStream(final SerialPort serialPort) {
    this.serialPort = serialPort;
  }

  @Override
  public void write(final int b) throws IOException {
    try {
      serialPort.writeByte((byte) b);
    } catch (final SerialPortException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void write(final byte[] b) throws IOException {
    try {
      serialPort.writeBytes(b);
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
