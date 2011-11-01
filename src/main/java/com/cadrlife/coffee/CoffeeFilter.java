package com.cadrlife.coffee;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.cadrlife.coffee.compile.CachingCoffeeCompiler;
import com.cadrlife.coffee.concat.CoffeescriptConcatenate;
import com.cadrlife.coffee.internal.org.springframework.util.AntPathMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

/**
 * Filter to compile coffeescript on the fly, with concatenation support. Does
 * do caching yet.
 * 
 * This filter takes 3 parameters:
 * 
 * coffeeFiles. Required. Ant-style path to all coffee files
 * ex. /WEB-INF/js/*.coffee
 * 
 * concatenateRoot. Optional. Path to the root file to resolve dependencies and concatenate results.
 * Must be exact path to a single file.
 * ex. /WEB-INF/js/main.coffee
 * 
 * concatenateName. Optional. Path that maps to the concatenated source code.
 * ex. /js/app.js
 * 
 */
public class CoffeeFilter implements Filter {
	private String concatenateRoot = "";
	private String concatenateName = "";
	private String coffeeFiles = "";
	private boolean concatenationEnabled;
	private AntPathMatcher antPathMatcher = new AntPathMatcher();

	private CachingCoffeeCompiler compiler;

	private FilterConfig filterConfig;
	private ServletContext servletContext;

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		this.servletContext = this.filterConfig.getServletContext();
		compiler = new CachingCoffeeCompiler();
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
		HttpServletRequest httpReq = (HttpServletRequest) request;
		String requestURI = httpReq.getRequestURI();
		ServletContext servletContext = filterConfig.getServletContext();
		String contextPath = servletContext.getContextPath();
		if (!isEnabled() || !requestURI.endsWith(".js")) {
			chain.doFilter(request, response);
			return;
		}
		if (requestURI.startsWith(contextPath)) {
			requestURI = requestURI.substring(contextPath.length());
		}
		String coffeeRequestURI = requestURI.substring(0, requestURI.length() - 3) + ".coffee";
		if (concatenationEnabled && requestURI.equals(concatenateName) && concatRootExists(servletContext)) {
			response.setContentType("text/javascript");
			Supplier<String> coffeeSupplier = concatenateResourcesSupplier();
			String compiledCoffee = compiler.compile(requestURI, coffeeSupplier);
			response.getOutputStream().print(compiledCoffee);
			return;
		}
		String resourcePath = "/WEB-INF" + coffeeRequestURI;
		if (!antPathMatcher.match(coffeeFiles, resourcePath)) {
			chain.doFilter(request, response);
			return;
		}
		
		URL resourceUrl = servletContext.getResource(resourcePath);
		if (resourceUrl == null) {
			chain.doFilter(request, response);
			return;
		}
		
		response.setContentType("text/javascript");
		response.getOutputStream().print(compiler.compile(requestURI, urlAsStringSupplier(resourceUrl)));
		
	}

	private Supplier<String> urlAsStringSupplier(final URL resourceUrl) {
		return new Supplier<String>() {
			public String get() {
				try {
					InputStream in = resourceUrl.openStream();
					Closeables.closeQuietly(in);
					String result = CharStreams.toString(new InputStreamReader(in));
					return result;
				} catch (IOException e) {
					throw new RuntimeException();
				}
			}
		};
	}

	private boolean concatRootExists(ServletContext servletContext)
			throws MalformedURLException {
		return null != servletContext.getResource(concatenateRoot);
	}

	/*
	 * Determines whether the filter should attempt to answer requests.
	 * This could be overridden by a child class to disable dynamic compilation in production.
	 */
	protected boolean isEnabled() {
		return true;
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
		return resolver.getResourcePaths(coffeeFiles);
	}

	private Supplier<String> concatenateResourcesSupplier() throws IOException {
		return new Supplier<String> (){
			public String get() {
				try {
					Iterable<VirtualFile> rootFiles = resourcesToFiles(rootCoffeePaths());
					Iterable<VirtualFile> includeFiles = resourcesToFiles(allCoffeePaths());
					return concatenateFilesAsString(rootFiles, includeFiles);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
		
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


}