package com.cadrlife.coffee.concat;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

class DependencyResolver {
	private final Set<String> usedFiles = Sets.newHashSet();
	private final List<FileDef> allFileDefs;
	
	public DependencyResolver(List<FileDef> allFileDefs) {
		this.allFileDefs = allFileDefs;
	}
	
	/*
	 *  recursively resolve the dependencies of a file.  If it 
	 *  has no dependencies, return that file in an array.  Otherwise,
	 *  find the files with the needed classes and resolve their dependencies
	 */
	public List<FileDef> resolve(FileDef fileDef) {
		List<FileDef> dependencyStack = Lists.newArrayList();
		if (usedFiles.contains(fileDef.getName())) {
			return dependencyStack;
		}
		if (fileDef.getDependencies().isEmpty() && fileDef.getFileDependencies().isEmpty()) {
			dependencyStack.add(fileDef);
			usedFiles.add(fileDef.getName());
		} else {
			addClassDependenciesToStack(fileDef, dependencyStack);
			addFileDependenciesToStack(fileDef, dependencyStack);
			if (!usedFiles.contains(fileDef.getName())) {
				dependencyStack.add(fileDef);
				usedFiles.add(fileDef.getName());
			}
		}
		return dependencyStack;
	}

	private void addClassDependenciesToStack(FileDef fileDef,
			List<FileDef> dependencyStack) {
		for (String dependency : fileDef.getDependencies()) {
			FileDef depFileDef = findFileDefForClass(allFileDefs, dependency);
			if (null == depFileDef) {
				System.err.println("Couldn't find class " + dependency + ", needed by " + fileDef.getName());
			} else {
				List<FileDef> nextStack = resolve(depFileDef);
				if (null != nextStack) {
					dependencyStack.addAll(nextStack);
				}
				
			}
			
		}
	}

	private void addFileDependenciesToStack(FileDef fileDef,
			List<FileDef> dependencyStack) {
		for (String neededFile : fileDef.getFileDependencies()) {
			FileDef neededFileDef = findFileDefByName(neededFile);
			
			if (null == neededFileDef) {
				System.err.println("Couldn't find file " + neededFile + ", required by file " + fileDef.getName());
			} else {
				List<FileDef> nextStack = resolve(neededFileDef);
				if (null != nextStack) {
					dependencyStack.addAll(nextStack);
				}
			}
		}
	}
	
	/*
	 * Given a class name, find the file that contains that
	 * class definition.  If it doesn't exist or we don't know
	 * about it, return null
	 */
	private FileDef findFileDefForClass(final List<FileDef> allFileDefs, String className) {
		for (FileDef fileDef : allFileDefs) {
			if (fileDef.getClasses().contains(className)) {
				return fileDef;
			}
		}
		return null;
	}
	
	/* Given a filename, find the file definition that
	 * corresponds to it.  If the file isn't found,
	 * return null
	 */
	private FileDef findFileDefByName(String fileName) {
		for (FileDef fileDef : allFileDefs) {
			if (fileName.equals(fileDef.getName())) {
				return fileDef;
			}
		}
		return null;
	}
}
