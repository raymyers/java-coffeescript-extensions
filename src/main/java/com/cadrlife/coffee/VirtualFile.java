package com.cadrlife.coffee;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import com.google.common.io.CharStreams;

public abstract class VirtualFile {
	
	private final String path;
	
	public static VirtualFile fromURL(String path, URL url) {
		return new URLVirtualFile(path, url);
	}
	
	public static VirtualFile fromFile(File file) {
		return new FileVirtualFile(file);
	}
	
	public abstract InputStream openInputStream() throws IOException;
	
	public String readToString(Charset charset) throws IOException {
		return CharStreams.toString(new InputStreamReader(this.openInputStream(),charset));
	}
	
	public String readToString() throws IOException {
		return readToString(Charset.defaultCharset());
	}

	
	public String getName() {
		String[] parts = getPath().split("[\\\\/]");
		return parts[parts.length-1];
	}
	
	protected VirtualFile(String path) {
		this.path= path;
	}

	public String getPath() {
		return path;
	}

	private static class URLVirtualFile extends VirtualFile{
		private final URL url;

		public URLVirtualFile(String path, URL url) {
			super(path);
			this.url = url;
		}

		@Override
		public InputStream openInputStream() throws IOException {
			return url.openStream();
		}
	}
	
	private static class FileVirtualFile extends VirtualFile{
		private File file;

		public FileVirtualFile(File file) {
			super(file.getPath());
			this.file = file;
		}

		@Override
		public InputStream openInputStream() throws IOException {
			return new FileInputStream(file);
		}
	}

}

