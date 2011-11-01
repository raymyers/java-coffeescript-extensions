package com.cadrlife.coffee.concat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import com.cadrlife.coffee.VirtualFile;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;


public class CoffeescriptConcatenate {
	CoffeescriptDependencyScanner dependencyScanner = new CoffeescriptDependencyScanner();
	
	
	public String concatenate(Iterable<VirtualFile> rootFiles, Iterable<VirtualFile> includeFiles) throws IOException {
		List<FileDef> deps = mapDependencies(rootFiles, includeFiles);
		String output = concatFiles(rootFiles, deps);
		return dependencyScanner.removeIncludeDirectives(output);
	}
	
	/* Given a list of files and their class/dependency information,
	 * traverse the list and put them in an order that satisfies dependencies. 
	 * Walk through the list, taking each file and examining it for dependencies.
	 * If it doesn't have any it's fit to go on the list.  If it does, find the file(s)
	 * that contain the classes dependencies.  These must go first in the hierarchy.
	 */
	public String concatFiles(Iterable<VirtualFile> sourceFiles, final List<FileDef> fileDefs) {
		
		final Iterable<String> sourceFileNames = collectFilenames(sourceFiles);
 		Stack<FileDef> sourceFileDefs = new Stack<FileDef>();
 		sourceFileDefs.addAll(findFileDefsWithNames(fileDefs, sourceFileNames));
 		
 		List<FileDef> fileDefStack = Lists.newArrayList();
 		// reuse dependencyResolver to preserve usedfiles list 
 		DependencyResolver dependencyResolver = new DependencyResolver(fileDefs);
 		while (!sourceFileDefs.isEmpty()) {
 			FileDef nextFileDef = sourceFileDefs.pop();
 			
 			
			List<FileDef> resolvedDef = dependencyResolver.resolve(nextFileDef);
 			fileDefStack.addAll(resolvedDef);
 		}
 		StringBuilder contentBuilder = new StringBuilder();
 		for (FileDef nextFileDef : fileDefStack) {
 			contentBuilder.append(nextFileDef.getContents() + '\n');
 		}
 		return contentBuilder.toString();		

	}
	
	/* Given a list of source files, 
	 * create a list of all files with the classes they contain and the classes 
	 * those classes depend on.
	 */
	public List<FileDef> mapDependencies(Iterable<VirtualFile> rootFiles, Iterable<VirtualFile> includeFiles) throws IOException {
		List<FileDef> fileDefs = Lists.newArrayList();
		for (VirtualFile sourceFile : Iterables.concat(rootFiles, includeFiles)) {
			String contents = sourceFile.readToString(Charset.defaultCharset());
			List<String> classes = dependencyScanner.findClasses(contents);
			List<String> fileDeps = dependencyScanner.findFileDependencies(contents);
			Collection<String> classDeps = dependencyScanner.findClassDependencies(contents);
			classDeps = Collections2.filter(classDeps, not(in(classes)));
			FileDef fileDef = new FileDef();
			fileDef.setName(stripCoffeeSuffix(sourceFile.getName()));
			fileDef.setClasses(classes);
			fileDef.setDependencies(Lists.newArrayList(classDeps));
			fileDef.setFileDependencies(fileDeps);
			fileDef.setContents(contents);
			fileDefs.add(fileDef);
		}
		return fileDefs;
	}

	private String stripCoffeeSuffix(String fileName) {
		return fileName.replaceAll("\\.coffee$", "");
	}
	
	private Collection<FileDef> findFileDefsWithNames(List<FileDef> fileDefs,
			final Iterable<String> sourceFileNames) {
		return Collections2.filter(fileDefs, new Predicate<FileDef>() {
			public boolean apply(FileDef input) {
				return Iterables.contains(sourceFileNames, input.getName());
			}
			
		});
	}

	private Iterable<String> collectFilenames(Iterable<VirtualFile> sourceFiles) {
		final Iterable<String> sourceFileNames = Iterables.transform(sourceFiles, new Function<VirtualFile, String>() {
			public String apply(VirtualFile f) {
				return stripCoffeeSuffix(f.getName());
				
			}
		});
		return sourceFileNames;
	}
	
	
	
}
