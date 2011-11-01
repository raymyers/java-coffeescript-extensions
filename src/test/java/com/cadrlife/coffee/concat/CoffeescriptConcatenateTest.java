package com.cadrlife.coffee.concat;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.cadrlife.coffee.VirtualFile;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import static org.junit.Assert.assertEquals;

public class CoffeescriptConcatenateTest {

	CoffeescriptConcatenate concat;
	List<VirtualFile> rootFiles;
	List<VirtualFile> includeFiles;
	VirtualFile noDeps;
	VirtualFile animal;
	String noDepsString;
	String animalString;
	VirtualFile snake;
	String snakeString;
	VirtualFile python;
	String pythonString;
	VirtualFile requireDirective;
	
	@Before
	public void setup() throws Exception {
		concat = new CoffeescriptConcatenate();
		rootFiles = Lists.newArrayList();
		includeFiles = Lists.newArrayList();
		noDeps = classpathFile("no-deps.coffee");
		animal = classpathFile("animal.coffee");
		snake = classpathFile("snake.coffee");
		python = classpathFile("python.coffee");
		requireDirective = classpathFile("require-directive.coffee");
		
		noDepsString = noDeps.readToString();
		animalString = animal.readToString();
		snakeString = snake.readToString();
		pythonString = python.readToString();
	}
	
	@Test
	public void shouldCompileZeroFiles() throws Exception {
		assertEquals("", concat());
	}
	
	@Test
	public void shouldCompileOneFileToFilesContents() throws Exception {
		rootFiles.add(noDeps);
		assertEquals(noDeps.readToString().trim(), concat().trim());
	}
	
	@Test
	public void shouldNotIncludeUnneededFiles() throws Exception {
		rootFiles.add(noDeps);
		includeFiles.add(animal);
		assertEquals(noDepsString.trim(), concat().trim());
	}
	
	@Test
	public void shouldNotIncludeRootFileTwice() throws Exception {
		rootFiles.add(noDeps);
		includeFiles.add(noDeps);
		assertEquals(noDepsString.trim(), concat().trim());
	}
	
	@Test
	public void shouldIncludeClassDependecy() throws Exception {
		rootFiles.add(snake);
		includeFiles.add(animal);
		assertEquals(animalString + "\n" + snakeString, concat().trim());
	}
	
	@Test
	public void shouldIncludeClassDependencyChainInInheritanceOrder() throws Exception {
		rootFiles.add(python);
		includeFiles.add(snake);
		includeFiles.add(animal);
		assertEquals(animalString + "\n" + snakeString +  "\n" + pythonString.trim(), concat().trim());
	}
	
	@Test
	public void requireDirectiveForFileAndClass() throws Exception {
		rootFiles.add(requireDirective);
		includeFiles.add(animal);
		includeFiles.add(snake);
		includeFiles.add(noDeps);
		includeFiles.add(python);
		assertEquals(animalString + "\n" + noDepsString.trim(), concat().trim());
	}

	private VirtualFile classpathFile(String fileName) {
		URL url = Resources.getResource(this.getClass(), fileName);
		return VirtualFile.fromURL(fileName, url);
	}

	private String concat() throws IOException {
		return concat.concatenate(rootFiles, includeFiles);
	}
	
}
