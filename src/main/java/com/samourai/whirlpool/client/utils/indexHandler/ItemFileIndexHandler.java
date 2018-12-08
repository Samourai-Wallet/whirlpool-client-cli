package com.samourai.whirlpool.client.utils.indexHandler;

public class ItemFileIndexHandler implements IIndexHandler {
  private FileIndexHandler fileIndexHandler;
  private String key;

  public ItemFileIndexHandler(FileIndexHandler fileIndexHandler, String key) {
    this.fileIndexHandler = fileIndexHandler;
    this.key = key;
  }

  @Override
  public int get() {
    return fileIndexHandler.get(key);
  }

  @Override
  public int getAndIncrement() {
    return fileIndexHandler.getAndIncrement(key);
  }

  @Override
  public void set(int value) {
    fileIndexHandler.set(key, value);
  }
}
