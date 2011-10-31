package com.cadrlife.coffee.concat;

import java.util.List;

import com.google.common.base.Objects;

class FileDef {
	private String name;
	private List<String> classes;
	private List<String> dependencies;
	private List<String> fileDependencies;
	private String contents;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getClasses() {
		return classes;
	}
	public void setClasses(List<String> classes) {
		this.classes = classes;
	}
	public List<String> getDependencies() {
		return dependencies;
	}
	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
	}
	public List<String> getFileDependencies() {
		return fileDependencies;
	}
	public void setFileDependencies(List<String> fileDependencies) {
		this.fileDependencies = fileDependencies;
	}
	public String getContents() {
		return contents;
	}
	public void setContents(String contents) {
		this.contents = contents;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", name).add("contents", contents).add("classes", classes).add("dependencies", dependencies).add("fileDependencies", fileDependencies).toString();
	}
	
}
