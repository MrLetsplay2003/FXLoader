package me.mrletsplay.fxloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FXLoader {

	private static final String
		MAVEN_REPO_JAR_URL = "https://repo1.maven.org/maven2/org/openjfx/{artifact}/{version}/{artifact}-{version}{classifier}.jar",
		CLASSIFIER;

	private static File libDirectory = new File("lib");
	private static HttpClient httpClient = HttpClient.newHttpClient();

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.contains("windows")) {
			CLASSIFIER = "win";
		}else if(osName.contains("mac") || osName.contains("darwin")) {
			CLASSIFIER = "mac";
		}else {
			CLASSIFIER = "linux";
		}
	}

	public static void setLibDirectory(File libDirectory) {
		FXLoader.libDirectory = libDirectory;
	}

	public static File getLibDirectory() {
		return libDirectory;
	}

	public static List<Path> downloadDependency(String artifact, String version) {
		String partialURL = MAVEN_REPO_JAR_URL
				.replace("{artifact}", artifact)
				.replace("{version}", version);

		libDirectory.mkdirs();

		List<Path> paths = new ArrayList<>();

		HttpRequest noClassifier = HttpRequest.newBuilder(URI.create(partialURL.replace("{classifier}", ""))).build();
		try {
			Path filePath = Paths.get(libDirectory.getPath(), artifact + ".jar");
			if(!Files.exists(filePath)) {
				log("Downloading " + artifact + "...");
				HttpResponse<byte[]> r = httpClient.send(noClassifier, BodyHandlers.ofByteArray());
				if(r.statusCode() != 200) {
					log("Failed to download dependency '" + artifact + "'");
					return null;
				}

				Files.write(filePath, r.body());
			}

			paths.add(filePath);
		} catch (IOException | InterruptedException e) {
			throw new FXLoaderException("Failed to download dependency '" + artifact + "'", e);
		}

		HttpRequest withClassifier = HttpRequest.newBuilder(URI.create(partialURL.replace("{classifier}", "-" + CLASSIFIER))).build();
		try {
			Path filePath = Paths.get(libDirectory.getPath(), artifact + "-" + CLASSIFIER + ".jar");
			if(!Files.exists(filePath)) {
				log("Downloading " + artifact + " (native code)...");
				HttpResponse<byte[]> r = httpClient.send(withClassifier, BodyHandlers.ofByteArray());
				if(r.statusCode() == 404) {
					log("Dependency " + artifact + " doesn't seem to have platform-specific code");
				}

				if(r.statusCode() != 200) {
					log("Failed to download dependency '" + artifact + "'");
					return null;
				}

				Files.write(filePath, r.body());
			}

			if(Files.exists(filePath)) paths.add(filePath);
		} catch (IOException | InterruptedException e) {
			throw new FXLoaderException("Failed to download dependency '" + artifact + "'", e);
		}

		return paths;
	}

	private static void log(String msg) {
		System.out.println("[FXLoader] " + msg);
	}

}
