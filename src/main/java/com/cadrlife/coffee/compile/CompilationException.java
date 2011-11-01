package com.cadrlife.coffee.compile;

import java.util.Arrays;
import java.util.List;
 
/**
 * A java compilation error
 */
public class CompilationException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String problem;
    private String source;
    private Integer line;
    private Integer start;
    private Integer end;
	private final String path;
 
 
    public CompilationException(String path, String source, String problem, int line, int start, int end) {
        super(problem);
		this.path = path;
        this.problem = problem;
        this.line = line;
        this.source = source;
        this.start = start;
        this.end = end;
    }
 
    public String getErrorTitle() {
        return String.format("Compilation error");
    }
 
    public String getErrorDescription() {
        return String.format("The file %s could not be compiled.\nError raised is : %s\n%s", isSourceAvailable() ? path : "", problem, getSourceDisplay());
    }
     
    @Override
    public String getMessage() {
        return getErrorDescription();
    }
 
    public List<String> getSourceDisplay() {
        if(start != -1 && end != -1) {
            if(start.equals(end)) {
                source = source.substring(0, start + 1) + "↓" + source.substring(end + 1);
            } else {
                source = source.substring(0, start) + "\000" + source.substring(start, end + 1) + "\001" + source.substring(end + 1);
            }
        }
        return Arrays.asList(source.split("\n"));
    }
 
    public Integer getLineNumber() {
        return line;
    }
 
    public String getSourceFile() {
        return path;
    }
 
    public Integer getSourceStart() {
        return start;
    }
 
    public Integer getSourceEnd() {
        return end;
    }
 
    public boolean isSourceAvailable() {
        return source != null && line != null;
    }
     
}