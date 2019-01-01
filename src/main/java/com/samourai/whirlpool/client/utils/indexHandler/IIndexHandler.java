package com.samourai.whirlpool.client.utils.indexHandler;

public interface IIndexHandler {
  int DEFAULT_VALUE = 0;

  int getAndIncrement();

  int get();

  void set(int value);
}
