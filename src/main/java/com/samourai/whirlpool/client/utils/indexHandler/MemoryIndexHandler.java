package com.samourai.whirlpool.client.utils.indexHandler;

public class MemoryIndexHandler implements IIndexHandler {
  private int index;

  public MemoryIndexHandler() {
    this(IIndexHandler.DEFAULT_VALUE);
  }

  public MemoryIndexHandler(int defaultValue) {
    index = defaultValue;
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
