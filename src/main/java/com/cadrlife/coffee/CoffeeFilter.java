package com.cadrlife.coffee;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.cadrlife.coffee.concat.CoffeescriptConcatenate;
import com.cadrlife.coffee.jcoffeescript.JCoffeeScriptCompileException;
import com.cadrlife.coffee.jcoffeescript.JCoffeeScriptCompiler;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;

/**
 * Filter to compile coffeescript on the fly, with concatenation support. Does
 * do caching yet.
 * 
 * This filter takes 3 parameters:
 * 
 * coffeeFiles. Required. Ant-style path to all coffee files
 * ex. /WEB-INF/js/*.coffee
 * 
 * concatenateRoot. Optional. Path to the root file to resolve dependencies and concatenate results
 * ex. /WEB-INF/js/main.coffee
 * 
 * concatenateName. Optional. Path that maps to the concatenated source code.
 * ex. /js/app.js
 * 
 * NOT YET IMPLEMENTED: cacheUpdateSeconds. Optional. How often to force an update of the compiled coffeescript cache
 * ex. 600
 */
public class CoffeeFilter implements Filter {
	private String concatenateRoot = "";
	private String concatenateName = "";
	private String coffeeFiles = "";
	private boolean concatenationEnabled;

	private static final class CompiledCoffee {
		// public final Date dateCached; // Time this was put in the cache
		public final String output; // Compiled coffee

		public CompiledCoffee(Date date, String output) {
			// this.dateCached = date;
			this.output = output;
		}
	}

	// Regex to get the line number of the failure.
	private static final Pattern LINE_NUMBER = Pattern.compile("line ([0-9]+)");
	private static final ThreadLocal<JCoffeeScriptCompiler> compiler = new ThreadLocal<JCoffeeScriptCompiler>() {
		@Override
		protected JCoffeeScriptCompiler initialValue() {
			return new JCoffeeScriptCompiler();
		}
	};
	private Map<String, CompiledCoffee> cache; // Map of Relative Path ->
												// Compiled coffee
												// private Date lastCacheUpdate
												// = new Date(0l);
	private FilterConfig filterConfig;
	private ServletContext servletContext;

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		this.servletContext = this.filterConfig.getServletContext();
		cache = new HashMap<String, CompiledCoffee>();
		coffeeFiles = filterConfig.getInitParameter("coffeeFiles");
		concatenateRoot = filterConfig.getInitParameter("concatenateRoot");
		concatenateName = filterConfig.getInitParameter("concatenateName");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(coffeeFiles), "CoffeeFilter requires the 'coffeeFiles' parameter");
		concatenationEnabled = !(Strings.isNullOrEmpty(concatenateName) || Strings
				.isNullOrEmpty(concatenateRoot));
	}
	
	public void destroy() {

	}


	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (!isEnabled()) {
			chain.doFilter(request, response);
			return;
		}
		HttpServletRequest httpReq = (HttpServletRequest) request;
		String requestURI = httpReq.getRequestURI();
		ServletContext servletContext = filterConfig.getServletContext();
		String contextPath = servletContext.getContextPath();
		if (requestURI.startsWith(contextPath)) {
			requestURI = requestURI.substring(contextPath.length());
		}
		String coffeeRequestURI = requestURI;
		if (requestURI.endsWith(".js")) {
			coffeeRequestURI = requestURI.substring(0, requestURI.length() - 3)
					+ ".coffee";
		}
		InputStream stream = servletContext.getResourceAsStream("/WEB-INF"
				+ coffeeRequestURI);
		if (concatenationEnabled && requestURI.equals(concatenateName)) {
			response.setContentType("text/javascript");
			String coffee = concatenateResourcesAsString(
					rootCoffeePaths(), allCoffeePaths());
			String compiledCoffee = compileCoffeescript(coffeeRequestURI, coffee);
			response.getOutputStream().print(compiledCoffee);
			cache.put(requestURI,
					new CompiledCoffee(new Date(), compiledCoffee));
			return;
		}
		if (stream == null) {
			chain.doFilter(request, response);
			return;
		}
		response.setContentType("text/javascript");
		// Check the cache.
		CompiledCoffee cc = cache.get(requestURI);
		if (cc != null
		// && cc.sourceLastModified.equals(file.lastModified())
		) {
			response.getOutputStream().print(cc.output);
			return;
		}
		// Compile the coffee and return.
		String coffee = CharStreams.toString(new InputStreamReader(stream));
		String compiledCoffee = compileCoffeescript(coffeeRequestURI, coffee);
		response.getOutputStream().print(compiledCoffee);
		cache.put(requestURI, new CompiledCoffee(new Date(), compiledCoffee));
		// Render a nice error page?
	}

	/*
	 * Determines whether the filter should attempt to answer requests.
	 * This could be overridden by a child class to disable dynamic compilation in production.
	 */
	protected boolean isEnabled() {
		return true;
	}

	private String compileCoffeescript(String requestURI, String coffee) {
		try {
			return getCompiler().compile(coffee);
		} catch (JCoffeeScriptCompileException e) {
			e.printStackTrace();
			throw new CompilationException(requestURI, coffee,
					e.getMessage(), getLineNumber(e), -1, -1);
		}
	}

	private Iterable<String> rootCoffeePaths()
			throws IOException {
		ServletContextPatternResolver resolver = new ServletContextPatternResolver(
				servletContext);

		return resolver.getResourcePaths(concatenateRoot);
	}

	private Iterable<String> allCoffeePaths()
			throws IOException {
		ServletContextPatternResolver resolver = new ServletContextPatternResolver(
				servletContext);
		return resolver.getResourcePaths(concatenateRoot);
	}

	private String concatenateResourcesAsString(
			Iterable<String> rootResources,
			Iterable<String> includeResources) throws IOException {
		Iterable<VirtualFile> rootFiles = resourcesToFiles(rootResources);
		Iterable<VirtualFile> includeFiles = resourcesToFiles(includeResources);
		return concatenateFilesAsString(rootFiles, includeFiles);
	}

	private String concatenateFilesAsString(Iterable<VirtualFile> rootFiles,
			Iterable<VirtualFile> includeFiles) throws IOException {
		return new CoffeescriptConcatenate().concatenate(rootFiles, includeFiles);
	}

	private Iterable<VirtualFile> resourcesToFiles(Iterable<String> rootResources) {
		Function<String, VirtualFile> resourceToFile = new Function<String, VirtualFile>() {

			public VirtualFile apply(String path) {
				try {
					URL url = servletContext.getResource(path);
					return VirtualFile.fromURL(path, url);
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}

		};
		Iterable<VirtualFile> rootFiles = Iterables.transform(rootResources,
				resourceToFile);
		return rootFiles;
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

	public static JCoffeeScriptCompiler getCompiler() {
		return compiler.get();
	}

}