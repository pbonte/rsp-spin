package org.rspspin.core;

import java.util.List;
import java.util.StringJoiner;

public class ArgumentConstraintException extends Exception {
	private static final long serialVersionUID = 1L;
	private List<String> errors;
	
	public ArgumentConstraintException(List<String> errors){
		this.errors = errors;
	}
	
	public String getMessage(){
		StringJoiner sj = new StringJoiner("\n");
		errors.forEach(sj::add);
		return sj.toString();
	}
}
