package io.fixprotocol._2026.orchestra.repository;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the compiled class files in target/classes use bytecode version 52 (Java 8).
 * This ensures the main classes in the multi-release JAR remain compatible with Java 8 consumers.
 */
public class BytecodeVersionTest {

  /** Java 8 class file major version */
  private static final int JAVA_8_MAJOR_VERSION = 52;

  @Test
  public void mainClassesUseBytecodeVersion52() throws IOException {
    File classesDir = new File("target/classes");
    assertTrue(classesDir.exists(), "target/classes directory must exist");

    File[] classFiles = findClassFiles(classesDir);
    assertTrue(classFiles.length > 0, "No .class files found in target/classes");

    for (File classFile : classFiles) {
      int majorVersion = readMajorVersion(classFile);
      assertEquals(JAVA_8_MAJOR_VERSION, majorVersion,
          "Expected Java 8 bytecode (version 52) in " + classFile.getPath()
              + " but got version " + majorVersion);
    }
  }

  /**
   * Returns all .class files under the given directory, excluding META-INF/versions (the
   * versioned entries in the multi-release JAR are allowed to target a later release).
   */
  private File[] findClassFiles(File dir) {
    java.util.List<File> result = new java.util.ArrayList<>();
    collectClassFiles(dir, dir, result);
    return result.toArray(new File[0]);
  }

  private void collectClassFiles(File root, File current, java.util.List<File> result) {
    File[] children = current.listFiles();
    if (children == null) {
      return;
    }
    for (File child : children) {
      if (child.isDirectory()) {
        // Skip META-INF/versions — those entries may target later Java releases
        String relativePath = child.getPath().replace(root.getPath(), "").replace('\\', '/');
        if (relativePath.startsWith("/META-INF/versions")) {
          continue;
        }
        collectClassFiles(root, child, result);
      } else if (child.getName().endsWith(".class")) {
        result.add(child);
      }
    }
  }

  /**
   * Reads the major version number from a class file.
   * Class file format: magic (4 bytes), minor_version (2 bytes), major_version (2 bytes).
   */
  private int readMajorVersion(File classFile) throws IOException {
    try (DataInputStream dis = new DataInputStream(new FileInputStream(classFile))) {
      int magic = dis.readInt();
      if (magic != 0xCAFEBABE) {
        throw new IOException("Not a valid class file: " + classFile.getPath());
      }
      dis.readUnsignedShort(); // minor version
      return dis.readUnsignedShort(); // major version
    }
  }
}
