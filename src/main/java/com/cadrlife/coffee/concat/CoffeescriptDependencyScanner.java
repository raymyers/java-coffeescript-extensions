package com.cadrlife.coffee.concat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;


/*
 * Used by Coffeescript concatenate to extract information about dependencies of a file.
 */
class CoffeescriptDependencyScanner {
	private final String fileDirectiveRegex = "#=\\s*require\\s*<([A-Za-z_$-][A-Za-z0-9_$-.]*)>";
	private final Pattern fileDirectivePattern = Pattern.compile(fileDirectiveRegex, Pattern.MULTILINE);
	private final String classDirectiveRegex = "#=\\s*require\\s+([A-Za-z_$-][A-Za-z0-9_$-]*)";
	private final Pattern classDirectivePattern = Pattern.compile(classDirectiveRegex, Pattern.MULTILINE);
	/* 
	 * Search through a file and find all class definitions,
	 * ignoring those in comments
	*/
	public List<String> findClasses(String file) {
		List<String> classNames = Lists.newArrayList();
		String classRegex = "^[^#\n]*class\\s([A-Za-z_$-][A-Za-z0-9_$-]*)";
		Matcher matcher = Pattern.compile(classRegex, Pattern.MULTILINE).matcher(file);
		while (matcher.find()) {
			classNames.add(matcher.group(1));
		}
		return classNames;
	}
	
	/* 
	 * Search through a file and find all dependencies,  
	 * which is be done by finding all 'exends' statements. 
	 * Ignore those in comments also find the dependencies 
	 * marked by #= require ClassName
	*/
	public List<String> findClassDependencies(String file) {
		List<String> dependencies = Lists.newArrayList();
		String extendsRegex = "^[^#\n]*extends\\s([A-Za-z_$-][A-Za-z0-9_$-]*)";
		Pattern extendsPattern = Pattern.compile(extendsRegex, Pattern.MULTILINE);
		Matcher extendsMatcher = extendsPattern.matcher(file);
		while (extendsMatcher.find()) {
			dependencies.add(extendsMatcher.group(1));
		}
		file = file.replaceAll(extendsRegex, "");
		
		Matcher classDirectiveMatcher = classDirectivePattern.matcher(file);
		
		while (classDirectiveMatcher.find()) {
			dependencies.add(classDirectiveMatcher.group(1));
		}
		return dependencies;
		
	}
	
	/*
	 * Search through a file, given as a string and find the dependencies marked by 
	 * #= require <FileName>
	 * those classes depend on.
	 */
	public List<String> findFileDependencies(String file) {
		List<String> dependencies = Lists.newArrayList();
		
		Matcher fileDirectiveMatcher = fileDirectivePattern.matcher(file);
		
		while (fileDirectiveMatcher.find()) {
			String fileName = fileDirectiveMatcher.group(1);
			dependencies.add(stripCoffeeSuffix(fileName));
		}
		return dependencies;
	}
	
	/*
	 * Remove all '#=require' directives, used after the files have been included.
	 */
	public String removeIncludeDirectives(String input) {
		String strippedInput = fileDirectivePattern.matcher(input).replaceAll("");
		return classDirectivePattern.matcher(strippedInput).replaceAll("");
	}
	
	private String stripCoffeeSuffix(String fileName) {
		return fileName.replaceAll("\\.coffee$", "");
	}
}
