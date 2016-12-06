package main.java.servermanagement.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import model.ConfigModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigUtils {
  private static final String CONFIG_FILE_NAME = "config.js";

  public static ConfigModel loadConfigJson() {
    ConfigModel config = null;

    try {
      String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE_NAME)));

      GsonBuilder builder = new GsonBuilder();
      Gson gson = builder.create();
      config = gson.fromJson(content, ConfigModel.class);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return config;
  }
}
