package org.rspspin.util;

import java.util.List;

public class ArgumentConstraintException extends Exception {
	private static final long serialVersionUID = 1L;
	private List<String> errors;
	
	public ArgumentConstraintException(List<String> errors){
		this.errors = errors;
	}
	
	public String getMessage(){
		StringBuilder sb = new StringBuilder();
		for(String e : errors){
			sb.append("\n");
			sb.append(e);
		}
		return sb.toString();
	}
}
