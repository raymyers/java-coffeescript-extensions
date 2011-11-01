package com.cadrlife.coffee.compile;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cadrlife.coffee.jcoffeescript.JCoffeeScriptCompileException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

/*
 * Caches by both source code and filename. Will recompile when the source code has changed.
 * Stores up to 100 files by default. This can be changed by passing a custom CacheBuilder to the constructor.
 */
public class CachingCoffeeCompiler {
	public static class CacheOptions {
		public int maxSize = 100;
		public int expirationTime = 10;
		public TimeUnit expirationTimeUnit = TimeUnit.MINUTES;
	}
	private final Cache<CompilationCacheFilenameKey, String> cache;
	
	// Regex to get the line number of the failure.
	private static final Pattern LINE_NUMBER = Pattern.compile("line ([0-9]+)");
	private ThreadSafeCoffeeScriptCompiler compiler;
	
	public CachingCoffeeCompiler() {
		this(new CacheOptions());
	}
	
	public CachingCoffeeCompiler(CacheOptions cacheOptions) {
		this(cacheOptions, new ThreadSafeCoffeeScriptCompiler());
	}
	
	CachingCoffeeCompiler(CacheOptions cacheOptions, ThreadSafeCoffeeScriptCompiler compiler) {
		this.compiler = compiler;
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(cacheOptions.maxSize)
				.expireAfterWrite(cacheOptions.expirationTime, cacheOptions.expirationTimeUnit)
				.build(new CoffeeCacheLoader());
	}
	

	/*
	 * This call will return the cached version if it exists, otherwise will
	 * block until compiler finishes. Will only invoke the supplier as needed.OsOs
	 */
	public String compile(String requestURI, Supplier<String> stringSupplier) {
		CompilationCacheFilenameKey key = new CompilationCacheFilenameKey();
		key.filename = requestURI;
		key.sourceCodeSupplier = stringSupplier;
		return cache.getUnchecked(key);
	}

	public String compile(String requestURI, String coffee) {
		return compile(requestURI, Suppliers.ofInstance(coffee));
	}

	/**
	 * @return the line number that the exception happened on, or 0 if not found
	 *         in the message.
	 */
	public static int getLineNumber(JCoffeeScriptCompileException e) {
		Matcher m = LINE_NUMBER.matcher(e.getMessage());
		if (m.find()) {
			return Integer.parseInt(m.group(1));
		}
		return 0;
	}

	private final class CoffeeCacheLoader extends CacheLoader<CompilationCacheFilenameKey, String> {
		@Override
		public String load(CompilationCacheFilenameKey request) throws Exception {
			String sourceCode = request.sourceCodeSupplier.get();
			try {
				System.out.println("cmp " + request.filename);
				return compiler.compile(sourceCode);
			} catch (JCoffeeScriptCompileException e) {
				e.printStackTrace();
				throw new CompilationException(request.filename,
						sourceCode, e.getMessage(), getLineNumber(e),
						-1, -1);
			}
		}
	}

}
