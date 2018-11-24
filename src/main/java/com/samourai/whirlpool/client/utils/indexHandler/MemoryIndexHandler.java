package com.samourai.whirlpool.client.utils.indexHandler;

public class MemoryIndexHandler implements IIndexHandler {
  private int index;

  public MemoryIndexHandler() {
    index = 0;
  }

  @Override
  public int get() {
    return index;
  }

  @Override
  public int getAndIncrement() {
    int result = index;
    index++;
    return result;
  }

  @Override
  public void set(int value) {
    index = value;
  }
}
