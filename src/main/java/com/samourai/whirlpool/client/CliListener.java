package com.samourai.whirlpool.client;

import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;

public class CliListener extends LoggingWhirlpoolClientListener {
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
  public void success(int nbMixs, MixSuccess mixSuccess) {
    //super.success(nbMixs, mixSuccess); // no log at success
    done = true;

    // override with custom code here: all mixs success
  }

  @Override
  public void fail(int currentMix, int nbMixs) {
    super.fail(currentMix, nbMixs);
    done = true;

    // override with custom code here: failure
  }

  @Override
  public void progress(
      int currentMix, int nbMixs, MixStep step, String stepInfo, int stepNumber, int nbSteps) {
    super.progress(currentMix, nbMixs, step, stepInfo, stepNumber, nbSteps);

    // override with custom code here: mix progress
  }

  @Override
  public void mixSuccess(int currentMix, int nbMixs, MixSuccess mixSuccess) {
    super.mixSuccess(currentMix, nbMixs, mixSuccess);

    // override with custom code here: one mix success (check if more mixs remaining with
    // currentMix==nbMixs)
  }
}
