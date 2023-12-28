package me.mrletsplay.fxloader.launcher;

import java.net.URL;
import java.net.URLClassLoader;

public class FXLoaderClassLoader extends URLClassLoader {

	private ClassLoader parent;
	private boolean debug;

	public FXLoaderClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, (ClassLoader) null);
		this.parent = parent;
		this.debug = System.getProperty("fxloader.debugClassLoader") != null;
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			if(debug) System.out.println("Find " + name);
			Class<?> c = super.findClass(name);
			if(debug) System.out.println(name + " (found) -> " + c);
			return c;
		}catch(ClassNotFoundException e) {
			return parent.loadClass(name);
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if(name.startsWith("me.mrletsplay.fxloader") || name.startsWith("me.mrletsplay.fxbuild")) return parent.loadClass(name);
		if(debug) System.out.println("Load " + name);
		Class<?> c = super.loadClass(name);
		if(debug) System.out.println(name + " (loaded) -> " + c);
		return c;
	}

	@Override
	public URL findResource(String name) {
		if(debug) System.out.println("Find resource" + name);
		URL url = super.findResource(name);
		if(debug && url != null) System.out.println(name + " (found resource) -> " + url);
		return url != null ? url : parent.getResource(name);
	}

}
