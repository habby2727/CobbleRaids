package com.kingpixel.cobbleraids.util;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.UtilsFile;

import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModFiles {
  private ModFiles() {
  }

  public static Gson gson() {
    return UtilsFile.getGson();
  }

  public static Path root() {
    return CobbleUtils.getPath().resolve(CobbleRaids.MOD_ID);
  }

  public static Path languageRoot() {
    return root().resolve("lang");
  }

  public static Path resolve(String first, String... more) {
    Path path = root().resolve(first);
    for (String part : more) {
      path = path.resolve(part);
    }
    return path;
  }

  public static List<Path> files(Path directory) {
    try {
      ensureDirectory(directory);
      List<Path> files = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
        for (Path path : stream) {
          if (Files.isRegularFile(path)) {
            files.add(path);
          }
        }
      }
      files.sort(Comparator.comparing(path -> path.getFileName().toString()));
      return files;
    } catch (IOException e) {
      throw new IllegalStateException("Could not list files in " + directory + ".", e);
    }
  }

  public static void ensureDirectory(Path directory) throws IOException {
    Files.createDirectories(directory);
  }

  public static String baseName(Path path) {
    String name = path.getFileName().toString();
    int extensionIndex = name.lastIndexOf('.');
    return extensionIndex >= 0 ? name.substring(0, extensionIndex) : name;
  }
}
