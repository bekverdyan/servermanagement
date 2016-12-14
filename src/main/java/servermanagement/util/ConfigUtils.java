package main.java.servermanagement.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import model.ConfigModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigUtils {

  public static ConfigModel loadConfigJson(String path) {
    ConfigModel config = null;

    try {
      String content = new String(Files.readAllBytes(Paths.get(path)));

      GsonBuilder builder = new GsonBuilder();
      Gson gson = builder.create();
      config = gson.fromJson(content, ConfigModel.class);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return config;
  }
}
