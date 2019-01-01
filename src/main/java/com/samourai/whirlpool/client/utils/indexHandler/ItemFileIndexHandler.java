package com.samourai.whirlpool.client.utils.indexHandler;

public class ItemFileIndexHandler implements IIndexHandler {
  private FileIndexHandler fileIndexHandler;
  private String key;
  private int defaultValue;

  public ItemFileIndexHandler(FileIndexHandler fileIndexHandler, String key, int defaultValue) {
    this.fileIndexHandler = fileIndexHandler;
    this.key = key;
    this.defaultValue = defaultValue;
  }

  @Override
  public int get() {
    return fileIndexHandler.get(key, defaultValue);
  }

  @Override
  public int getAndIncrement() {
    return fileIndexHandler.getAndIncrement(key, defaultValue);
  }

  @Override
  public void set(int value) {
    fileIndexHandler.set(key, value);
  }
}
