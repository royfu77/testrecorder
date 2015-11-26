package com.almondtools.testrecorder.visitors;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Computation {

	public static final Computation NULL = new Computation("");
	
	private List<String> statements;
	private String value;
	private Type type;
	private boolean stored;

	public Computation(String value) {
		this(value, null, false, new ArrayList<>());
	}

	public Computation(String value, boolean stored) {
		this(value, null, stored, new ArrayList<>());
	}

	public Computation(String value, List<String> statements) {
		this(value, null, false, statements);
	}
	
	public Computation(String value, Type type) {
		this(value, type, false, new ArrayList<>());
	}

	public Computation(String value, Type type, boolean stored) {
		this(value, type, stored, new ArrayList<>());
	}

	public Computation(String value, Type type, List<String> statements) {
		this(value, type, false, statements);
	}

	public Computation(String value, boolean stored, List<String> statements) {
		this(value, null, false, statements);
	}

	public Computation(String value, Type type, boolean stored, List<String> statements) {
		this.value = value;
		this.type = type;
		this.stored = stored;
		this.statements = statements;
	}
	
	public String getValue() {
		return value;
	}
	
	public Type getType() {
		return type;
	}
	
	public boolean isStored() {
		return stored;
	}
	
	public List<String> getStatements() {
		return statements;
	}

}
