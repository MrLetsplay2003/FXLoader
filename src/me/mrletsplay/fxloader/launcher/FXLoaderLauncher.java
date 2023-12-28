package me.mrletsplay.fxloader.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import me.mrletsplay.fxloader.FXLoader;
import me.mrletsplay.fxloader.FXLoaderException;

public class FXLoaderLauncher {

	public static void main(String[] args) {
		URL selfURL = FXLoaderLauncher.class.getProtectionDomain().getCodeSource().getLocation();
		String mainClass;
		String libDirectory;
		List<String> dependencies;
		try(InputStream in = FXLoader.class.getResourceAsStream("/META-INF/fxbuild/meta.txt")) {
			String[] meta = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n");
			mainClass = meta[0];
			dependencies = Arrays.asList(meta[1].split(";"));
			libDirectory = meta[2];
		} catch (IOException e) {
			throw new FXLoaderException("Failed to load metadata file", e);
		}

		FXLoader.setLibDirectory(new File(libDirectory));

		log("Main class: " + mainClass);
		log("Required dependencies: " + dependencies.stream().collect(Collectors.joining(", ")));
		log("Lib directory: " + libDirectory);

		log("Downloading dependencies...");

		List<URL> urls = new ArrayList<>();
		urls.add(selfURL);

		for(String dep : dependencies) {
			String[] spl = dep.split(":");
			FXLoader.downloadDependency(spl[0], spl[1]).forEach(p -> {
				try {
					urls.add(p.toUri().toURL());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			});
		}

		try {
			ClassLoader cl = new FXLoaderClassLoader(urls.toArray(URL[]::new), FXLoader.class.getClassLoader());
			Thread.currentThread().setContextClassLoader(cl);
			Class<?> main = cl.loadClass(mainClass);
			main.getMethod("main", String[].class).invoke(null, (Object) args);
		}catch(IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			throw new FXLoaderException("Failed to run program", e);
		}catch(InvocationTargetException e) {
			e.getTargetException().printStackTrace();
		}
	}

	private static void log(String msg) {
		System.out.println("[FXBuildLauncher] " + msg);
	}

}
