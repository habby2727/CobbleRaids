package com.kingpixel.cobbleraids.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.Method;

public final class GsonCompat {
  private static final Method FROM_JSON_STRING = findMethod(String.class);
  private static final Method FROM_JSON_ELEMENT = findMethod(JsonElement.class);

  private GsonCompat() {
  }

  public static Object fromJson(Gson gson, String json, Class<?> type) {
    return invoke(FROM_JSON_STRING, gson, json, type);
  }

  public static Object fromJson(Gson gson, JsonElement json, Class<?> type) {
    return invoke(FROM_JSON_ELEMENT, gson, json, type);
  }

  private static Method findMethod(Class<?> jsonType) {
    try {
      return Gson.class.getMethod("fromJson", jsonType, Class.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static Object invoke(Method method, Gson gson, Object json, Class<?> type) {
    try {
      return method.invoke(gson, json, type);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Could not deserialize JSON using Gson", e);
    }
  }
}
