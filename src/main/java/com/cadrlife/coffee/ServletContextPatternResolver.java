package com.cadrlife.coffee;

import java.util.Set;

import javax.servlet.ServletContext;

import com.cadrlife.coffee.internal.org.springframework.util.AntPathMatcher;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class ServletContextPatternResolver {
	private ServletContext servletContext;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();
	
	public ServletContextPatternResolver(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public Set<String> getResourcePaths(String pattern) {
		return getResourcePathsFromRoot("/", pattern);
	}
	
	private Set<String> getResourcePathsFromRoot(String root, final String pattern) {
		Set<String> results = Sets.newHashSet();
		@SuppressWarnings("unchecked")
		Set<String> paths = servletContext.getResourcePaths(root);

		results.addAll(filterFullMatches(pattern, paths));
		for (String startMatch : filterStartMatches(pattern, paths)) {
			results.addAll(getResourcePathsFromRoot(startMatch, pattern));
		}
		return results;
		
	}

	private Set<String> filterFullMatches(final String pattern, Set<String> paths) {
		return Sets.filter(paths, new Predicate<String>() {
			public boolean apply(String path) {
				return pathMatcher.match(pattern, path);
			}
		});
	}

	private Set<String> filterStartMatches(final String pattern, Set<String> paths) {
		return Sets.filter(paths, new Predicate<String>() {
			public boolean apply(String path) {
				return pathMatcher.matchStart(pattern, path);
			}
		});
	}
 
}
