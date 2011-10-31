package com.cadrlife.coffee.concat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cadrlife.coffee.VirtualFile;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;


public class CoffeescriptConcat {
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
	
	/* Given a list of source files, 
	 * create a list of all files with the classes they contain and the classes 
	 * those classes depend on.
	 */
	public List<FileDef> mapDependencies(Iterable<VirtualFile> rootFiles, Iterable<VirtualFile> includeFiles) throws IOException {
		List<FileDef> fileDefs = Lists.newArrayList();
		for (VirtualFile sourceFile : Iterables.concat(rootFiles, includeFiles)) {
			String contents = sourceFile.readToString(Charset.defaultCharset());
			List<String> classes = findClasses(contents);
			List<String> fileDeps = findFileDependencies(contents);
			Collection<String> classDeps = findClassDependencies(contents);
			classDeps = Collections2.filter(classDeps, not(in(classes)));
			FileDef fileDef = new FileDef();
			fileDef.setName(stripCoffeeSuffix(sourceFile.getName()));
			fileDef.setClasses(classes);
			fileDef.setDependencies(Lists.newArrayList(classDeps));
			fileDef.setFileDependencies(fileDeps);
			fileDef.setContents(contents);
			fileDefs.add(fileDef);
//			System.out.println(fileDef);
		}
		return fileDefs;
	}

	private String stripCoffeeSuffix(String fileName) {
		return fileName.replaceAll("\\.coffee$", "");
	}
	
	/* Given a list of files and their class/dependency information,
	 * traverse the list and put them in an order that satisfies dependencies. 
	 * Walk through the list, taking each file and examining it for dependencies.
	 * If it doesn't have any it's fit to go on the list.  If it does, find the file(s)
	 * that contain the classes dependencies.  These must go first in the hierarchy.
	 */
	public String concatFiles(Iterable<VirtualFile> sourceFiles, List<FileDef> fileDefs) {
		final List<String> usedFiles = Lists.newArrayList();
		final List<FileDef> allFileDefs = Lists.newArrayList(fileDefs);
		final Iterable<String> sourceFileNames = Iterables.transform(sourceFiles, new Function<VirtualFile, String>() {

			public String apply(VirtualFile f) {
				return stripCoffeeSuffix(f.getName());
				
			}
		});
 		Stack<FileDef> sourceFileDefs = new Stack<FileDef>();
 		sourceFileDefs.addAll(Collections2.filter(fileDefs, new Predicate<FileDef>() {
			public boolean apply(FileDef input) {
				return Iterables.contains(sourceFileNames, input.getName());
			}
			
		}));
 		// Given a class name, find the file that contains that
 		// class definition.  If it doesn't exist or we don't know
 		// about it, return null
 		final Function<String, FileDef> findFileDefByClass = new Function<String, FileDef>() {

			public FileDef apply(String className) {
				for (FileDef fileDef : allFileDefs) {
					if (fileDef.getClasses().contains(className)) {
						return fileDef;
					}
				}
				return null;
			}
 			
 		};
 		
 		// Given a filename, find the file definition that
 		// corresponds to it.  If the file isn't found,
 		// return null
 		final Function<String, FileDef> findFileDefByName = new Function<String, FileDef>() {

			public FileDef apply(String fileName) {
				for (FileDef fileDef : allFileDefs) {
					if (fileName.equals(fileDef.getName())) {
						return fileDef;
					}
				}
				return null;
			}
 		};
 		
 		// recursively resolve the dependencies of a file.  If it 
 		// has no dependencies, return that file in an array.  Otherwise,
 		// find the files with the needed classes and resolve their dependencies
 		final Function<FileDef, List<FileDef>> resolveDependencies = new Function<FileDef, List<FileDef>>() {

			public List<FileDef> apply(FileDef fileDef) {
				List<FileDef> dependencyStack = Lists.newArrayList();
				if (usedFiles.contains(fileDef.getName())) {
					return dependencyStack;
				}
				if (fileDef.getDependencies().isEmpty() && fileDef.getFileDependencies().isEmpty()) {
					dependencyStack.add(fileDef);
					usedFiles.add(fileDef.getName());
				} else {
					for (String dependency : fileDef.getDependencies()) {
						FileDef depFileDef = findFileDefByClass.apply(dependency);
						if (null == depFileDef) {
							System.err.println("Couldn't find class " + dependency + ", needed by " + fileDef.getName());
						} else {
							List<FileDef> nextStack = this.apply(depFileDef);
							if (null != nextStack) {
								dependencyStack.addAll(nextStack);
							}
							
						}
						
					}
					for (String neededFile : fileDef.getFileDependencies()) {
						String neededFilename = neededFile.replaceAll("\\.\\w*$", "");
						FileDef neededFileDef = findFileDefByName.apply(neededFilename);
						
						if (null == neededFileDef) {
							System.err.println("Couldn't find file " + neededFilename + ", needed by " + fileDef.getName());
						} else {
							List<FileDef> nextStack = this.apply(neededFileDef);
							if (null != nextStack) {
								dependencyStack.addAll(nextStack);
							}
						}
						
					}
					if (!usedFiles.contains(fileDef.getName())) {
						dependencyStack.add(fileDef);
						usedFiles.add(fileDef.getName());
					}
				}
				return dependencyStack;
			}
			
 		};
 		List<FileDef> fileDefStack = Lists.newArrayList();
 		while (!sourceFileDefs.isEmpty()) {
 			FileDef nextFileDef = sourceFileDefs.pop();
// 			System.out.println("nextFileDef = " + nextFileDef.getName());
 			List<FileDef> resolvedDef = resolveDependencies.apply(nextFileDef);
 			fileDefStack.addAll(resolvedDef);
 		}
 		StringBuilder contentBuilder = new StringBuilder();
 		for (FileDef nextFileDef : fileDefStack) {
 			contentBuilder.append(nextFileDef.getContents() + '\n');
 		}
 		return contentBuilder.toString();		

	}
	
	public String removeDirectives(String input) {
		String strippedInput = fileDirectivePattern.matcher(input).replaceAll("");
		return classDirectivePattern.matcher(strippedInput).replaceAll("");
	}
	
	public String concatenate(Iterable<VirtualFile> rootFiles, Iterable<VirtualFile> includeFiles) throws IOException {
		List<FileDef> deps = mapDependencies(rootFiles, includeFiles);
		String output = concatFiles(rootFiles, deps);
		return removeDirectives(output);
	}
	
	public static void main(String[] args) throws IOException {
		List<VirtualFile> root = Lists.newArrayList();
		List<VirtualFile> include = Lists.newArrayList();
		String jsDir = "/home/ray/Documents/workspace-sts-2.8.0.M2/spring-mvc-playground/src/main/webapp/WEB-INF/js/";
		File rootFile = new File(jsDir + "home.coffee");
		root.add(VirtualFile.fromFile(rootFile));
		include.add(VirtualFile.fromFile(new File(jsDir + "app/a1.coffee")));
		include.add(VirtualFile.fromFile(new File(jsDir + "app/a2.coffee")));
		
		System.out.println(new CoffeescriptConcat().concatenate(root, include));
	}

}
