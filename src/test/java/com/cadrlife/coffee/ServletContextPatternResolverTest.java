package com.cadrlife.coffee;

import java.util.Set;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ServletContextPatternResolverTest {
	@Mock
	ServletContext servletContext;
	
	@InjectMocks
	ServletContextPatternResolver resolver = new ServletContextPatternResolver(null);
	
	@Before
	public void setup() throws Exception {
		when(servletContext.getResourcePaths("/")).thenReturn(Sets.newHashSet("/file.txt","/file.js","/dir/"));
		when(servletContext.getResourcePaths("/dir/")).thenReturn(Sets.newHashSet("/dir/dirfile.txt","/dir/sub/"));
		when(servletContext.getResourcePaths("/dir/sub/")).thenReturn(Sets.newHashSet("/dir/sub/sub.txt","/dir/sub/sub.js"));
	}
	
	@Test
	public void extensionInRoot(){
		Set<String> results = resolver.getResourcePaths("/*.txt");
		
		String resultString = results.toString();
		assertTrue(resultString,results.contains("/file.txt"));
		assertFalse(resultString,results.contains("/file.js"));
		assertFalse(resultString,results.contains("/dir/"));
	}
	
	@Test
	public void wildCardInRoot(){
		Set<String> results = resolver.getResourcePaths("/*");
		assertTrue(results.contains("/file.txt"));
		assertTrue(results.contains("/file.js"));
	}
	
	@Test
	public void wildCardInDir() {
		Set<String> results = resolver.getResourcePaths("/*/*");
		
		String resultString = results.toString();
		assertTrue(resultString, results.contains("/dir/dirfile.txt"));
	}
	
	@Test
	public void extensionInSub() {
		Set<String> results = resolver.getResourcePaths("/*/sub/*.txt");
		
		String resultString = results.toString();
		assertTrue(resultString, results.contains("/dir/sub/sub.txt"));
	}
	
	@Test
	public void allInSub() {
		Set<String> results = resolver.getResourcePaths("/*/sub/*");
		
		String resultString = results.toString();
		assertTrue(resultString, results.contains("/dir/sub/sub.js"));
		assertTrue(resultString, results.contains("/dir/sub/sub.txt"));
	}
	
	@Test
	public void doubleWildCardSeesSubdirs() {
		Set<String> results = resolver.getResourcePaths("/**/*");
		
		String resultString = results.toString();
		assertTrue(resultString, results.contains("/dir/sub/sub.txt"));
		assertTrue(resultString, results.contains("/dir/sub/sub.js"));
		assertTrue(resultString, results.contains("/dir/dirfile.txt"));
		assertTrue(resultString, results.contains("/file.txt"));
	}
	
	
}
