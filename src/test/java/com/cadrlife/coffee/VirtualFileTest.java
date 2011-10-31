package com.cadrlife.coffee;

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.*;
public class VirtualFileTest {
	@Test
	public void pathToName() {
		VirtualFile file = VirtualFile.fromFile(new File("/a/b/c/file"));
		assertEquals("file", file.getName());
	}
}
