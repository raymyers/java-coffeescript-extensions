package com.cadrlife.coffee.concat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoffeescriptConcatTest {

	@Before
	public void setup() {
		
	}
	
	@Test
	public void findClasses() {
		List<String> classDefs = new CoffeescriptConcat().findClasses("class Horse extends Animal\nclass Animal");
		assertEquals("Horse", classDefs.get(0));
		assertEquals("Animal", classDefs.get(1));
	}
	
	@Test
	public void findClassDependenciesForSuperclasses() {
		List<String> classDefs = new CoffeescriptConcat().findClassDependencies("class Horse extends Animal\nclass Animal");
		assertEquals("Animal", classDefs.get(0));
	}
	
	@Test
	public void findClassDependenciesForRequireDirective() {
		List<String> classDefs = new CoffeescriptConcat().findClassDependencies("#= require ClassName");
		assertEquals("ClassName", classDefs.get(0));
	}
	
	@Test
	public void findFileDependencies() {
		List<String> classDefs = new CoffeescriptConcat().findFileDependencies("#= require <Filename.coffee>\n() ->  ");
		assertEquals("Filename", classDefs.get(0));
	}
	@Test
	public void findFileDependenciesNoSpace() {
		List<String> classDefs = new CoffeescriptConcat().findFileDependencies("#= require<Filename.coffee>");
		assertEquals("Filename", classDefs.get(0));
	}
	
	@Test
	public void findFileDependenciesNoSuffix() {
		List<String> classDefs = new CoffeescriptConcat().findFileDependencies("#= require <Filename>\n() ->  ");
		assertEquals("Filename", classDefs.get(0));
	}
}
