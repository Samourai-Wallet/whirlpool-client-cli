package com.samourai.whirlpool.client.utils.indexHandler;

public interface IIndexHandler {

  int getAndIncrement();

  int get();

  void set(int value);
}
