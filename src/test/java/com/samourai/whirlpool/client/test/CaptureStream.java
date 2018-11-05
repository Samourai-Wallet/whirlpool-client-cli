package com.samourai.whirlpool.client.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class CaptureStream extends ByteArrayOutputStream {
  private PrintStream printStream;

  public CaptureStream(PrintStream printStream) {
    this.printStream = printStream;
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) {
    super.write(b, off, len);

    printStream.println(new String(b));
  }
}
