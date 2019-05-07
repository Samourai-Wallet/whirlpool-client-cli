package com.samourai.whirlpool.cli;

import com.samourai.whirlpool.client.mix.listener.MixFailReason;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;

public class CliListener implements WhirlpoolClientListener {
  private boolean done;

  public CliListener() {
    super();
  }

  public void waitDone() throws InterruptedException {
    synchronized (this) {
      while (!done) {
        wait(1000);
      }
    }
  }

  @Override
  public void success(MixSuccess mixSuccess) {
    done = true;

    // override with custom code here: success
  }

  @Override
  public void fail(MixFailReason reason, String notifiableError) {
    done = true;

    // override with custom code here: failure
  }

  @Override
  public void progress(MixStep step) {

    // override with custom code here: mix progress
  }
}
