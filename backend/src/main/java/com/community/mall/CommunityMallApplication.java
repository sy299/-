package com.community.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class CommunityMallApplication {
  private static final int FRONTEND_PORT = 5173;
  private static Process frontendProcess;

  public static void main(String[] args) {
    SpringApplication.run(CommunityMallApplication.class, args);
    startFrontend();
  }

  private static void startFrontend() {
    if (isPortInUse(FRONTEND_PORT)) {
      System.out.println("Frontend is already running at http://localhost:" + FRONTEND_PORT);
      return;
    }

    Path frontendDirectory = findFrontendDirectory();
    if (frontendDirectory == null) {
      System.err.println("Frontend directory was not found. Backend started without Vite.");
      return;
    }

    boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
    List<String> command = windows
        ? List.of("cmd.exe", "/c", "npm.cmd", "run", "dev")
        : List.of("npm", "run", "dev");
    try {
      frontendProcess = new ProcessBuilder(command)
          .directory(frontendDirectory.toFile())
          .redirectErrorStream(true)
          .inheritIO()
          .start();
      Runtime.getRuntime().addShutdownHook(new Thread(CommunityMallApplication::stopFrontend, "frontend-shutdown"));
      System.out.println("Frontend starting at http://localhost:" + FRONTEND_PORT);
    } catch (IOException e) {
      System.err.println("Could not start frontend with npm: " + e.getMessage());
    }
  }

  private static Path findFrontendDirectory() {
    Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    List<Path> candidates = new ArrayList<>();
    candidates.add(current.resolve("frontend"));
    if (current.getParent() != null) candidates.add(current.getParent().resolve("frontend"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate) && Files.isRegularFile(candidate.resolve("package.json"))) return candidate;
    }
    return null;
  }

  private static boolean isPortInUse(int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }

  private static void stopFrontend() {
    if (frontendProcess == null || !frontendProcess.isAlive()) return;
    frontendProcess.descendants().forEach(ProcessHandle::destroy);
    frontendProcess.destroy();
  }
}
