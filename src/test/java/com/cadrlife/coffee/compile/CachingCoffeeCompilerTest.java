package com.cadrlife.coffee.compile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Supplier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CachingCoffeeCompilerTest {
	
	@Mock
	ThreadSafeCoffeeScriptCompiler compiler;
	
	@Mock
	Supplier<String> sourceSupplier1;
	
	@Mock
	Supplier<String> sourceSupplier2;
	
	@InjectMocks
	CachingCoffeeCompiler cachingCompiler = new CachingCoffeeCompiler();
	
	
	@Before
	public void setup() throws Exception {
		when(sourceSupplier1.get()).thenReturn("coffee1");
		when(sourceSupplier2.get()).thenReturn("coffee1");
		when(compiler.compile("coffee1")).thenReturn("js1");
		when(compiler.compile("coffee2")).thenReturn("js2");
	}
	
	@Test
	public void shouldCompile() throws Exception {
		assertEquals("js1", cachingCompiler.compile("", sourceSupplier1));
	}
	
	@Test
	public void shouldCompileOnlyOnceForSameFilename() throws Exception {
		when(compiler.compile("coffee")).thenReturn("js");
		assertEquals("js1", cachingCompiler.compile("uri", sourceSupplier1));
		assertEquals("js1", cachingCompiler.compile("uri", sourceSupplier2));
		verify(compiler, times(1)).compile("coffee1");
		verify(compiler, never()).compile("coffee2");
		verify(sourceSupplier1, times(1)).get();
		verify(sourceSupplier2, never()).get();
	}
	
	@Test
	public void recompileOnFilenameChange() throws Exception {
		when(compiler.compile("coffee")).thenReturn("js");
		assertEquals("js", cachingCompiler.compile("uri", "coffee"));
		assertEquals("js", cachingCompiler.compile("uri2", "coffee"));
		assertEquals("js", cachingCompiler.compile("uri2", "coffee"));
		verify(compiler, times(2)).compile("coffee");
	}
	
}
