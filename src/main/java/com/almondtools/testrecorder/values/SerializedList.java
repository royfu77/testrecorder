package com.almondtools.testrecorder.values;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.almondtools.testrecorder.SerializedCollectionVisitor;
import com.almondtools.testrecorder.SerializedValue;
import com.almondtools.testrecorder.SerializedValueVisitor;
import com.almondtools.testrecorder.visitors.SerializedValuePrinter;

public class SerializedList implements SerializedValue, List<SerializedValue> {

	private Type type;
	private List<SerializedValue> list;

	public SerializedList(Type type) {
		this.type = type;
		this.list = new ArrayList<>();
	}
	
	@Override
	public Type getType() {
		return type;
	}

	public Type getComponentType() {
		if (type instanceof ParameterizedType) {
			return ((ParameterizedType) type).getActualTypeArguments()[0];
		} else {
			return Object.class;
		}
	}

	@Override
	public <T> T accept(SerializedValueVisitor<T> visitor) {
		return visitor.as(SerializedCollectionVisitor.extend(visitor))
			.map(v -> v.visitList(this))
			.orElseGet(() -> visitor.visitUnknown(this));
	}

	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public Iterator<SerializedValue> iterator() {
		return list.iterator();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public boolean add(SerializedValue e) {
		return list.add(e);
	}

	public boolean remove(Object o) {
		return list.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean addAll(Collection<? extends SerializedValue> c) {
		return list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends SerializedValue> c) {
		return list.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public void clear() {
		list.clear();
	}

	public SerializedValue get(int index) {
		return list.get(index);
	}

	public SerializedValue set(int index, SerializedValue element) {
		return list.set(index, element);
	}

	public void add(int index, SerializedValue element) {
		list.add(index, element);
	}

	public SerializedValue remove(int index) {
		return list.remove(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator<SerializedValue> listIterator() {
		return list.listIterator();
	}

	public ListIterator<SerializedValue> listIterator(int index) {
		return list.listIterator(index);
	}

	public List<SerializedValue> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	public boolean equals(Object o) {
		return list.equals(o);
	}

	public int hashCode() {
		return list.hashCode();
	}

	@Override
	public String toString() {
		return accept(new SerializedValuePrinter());
	}

}