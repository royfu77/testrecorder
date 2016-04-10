package net.amygdalum.testrecorder.values;

import static java.util.Arrays.asList;
import static net.amygdalum.testrecorder.values.GenericTypes.arrayListOfSetOfString;
import static net.amygdalum.testrecorder.values.GenericTypes.arrayListOfString;
import static net.amygdalum.testrecorder.values.GenericTypes.listOfBounded;
import static net.amygdalum.testrecorder.values.GenericTypes.listOfString;
import static net.amygdalum.testrecorder.values.ParameterizedTypeMatcher.parameterized;
import static net.amygdalum.testrecorder.values.SerializedLiteral.literal;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.amygdalum.testrecorder.SerializedValue;
import net.amygdalum.testrecorder.deserializers.TestValueVisitor;

public class SerializedListTest {

	@Test
		public void testGetResultTypeRaw() throws Exception {
			assertThat(new SerializedList(ArrayList.class).withResult(List.class).getResultType(), equalTo(List.class));
		}

	@Test
		public void testGetResultTypeParameterized() throws Exception {
			assertThat(new SerializedList(arrayListOfString()).withResult(listOfString()).getResultType(), parameterized(List.class, String.class));
		}

	@Test
		public void testGetResultTypeIndirectParameterized() throws Exception {
			assertThat(new SerializedList(arrayListOfString()).withResult(arrayListOfString()).getResultType(), parameterized(ArrayList.class, String.class));
		}

	@Test
		public void testGetResultTypeBounded() throws Exception {
			assertThat(new SerializedList(ArrayList.class).withResult(listOfBounded()).getResultType(), instanceOf(TypeVariable.class));
		}

	@Test
	public void testGetComponentTypeRaw() throws Exception {
		assertThat(new SerializedList(ArrayList.class).withResult(listOfBounded()).getComponentType(), equalTo(Object.class));
	}

	@Test
	public void testGetComponentTypeParameterized() throws Exception {
		assertThat(new SerializedList(arrayListOfString()).withResult(listOfString()).getComponentType(), equalTo(String.class));
	}

	@Test
	public void testGetComponentTypeNestedParameterized() throws Exception {
		assertThat(new SerializedList(arrayListOfSetOfString()).getComponentType(), parameterized(Set.class, String.class));
	}

	@Test
	public void testGetComponentTypeIndirectParameterized() throws Exception {
		assertThat(new SerializedList(arrayListOfString()).getComponentType(), equalTo(String.class));
	}

	@Test
	public void testGetComponentTypeBounded() throws Exception {
		assertThat(new SerializedList(ArrayList.class).withResult(listOfBounded()).getComponentType(), equalTo(Object.class));
	}

	@Test
	public void testSize0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.size(), equalTo(0));
	}

	@Test
	public void testSize1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.size(), equalTo(1));
	}

	@Test
	public void testIsEmpty0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.isEmpty(), is(true));
	}

	@Test
	public void testIsEmpty1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.isEmpty(), is(false));
	}

	@Test
	public void testContains0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.contains(literal(String.class, "string")), is(false));
	}

	@Test
	public void testContains1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.contains(literal(String.class, "string")), is(true));
	}

	@Test
	public void testIterator0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.iterator().hasNext(), is(false));
	}

	@Test
	public void testIterator1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.iterator().next(), equalTo(literal(String.class, "string")));
	}

	@Test
	public void testToArray0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.toArray(), emptyArray());
		assertThat(list.toArray(new SerializedValue[0]), emptyArray());
	}

	@Test
	public void testToArray1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.toArray(), arrayContaining(literal(String.class, "string")));
		assertThat(list.toArray(new SerializedValue[0]), arrayContaining(literal(String.class, "string")));
	}

	@Test
	public void testRemoveObject0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.remove(literal(String.class, "string")), is(false));
	}

	@Test
	public void testRemoveObject1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.remove(literal(String.class, "string")), is(true));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testRemoveInt0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.remove(0);
	}

	@Test
	public void testRemoveInt1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.remove(0), equalTo(literal(String.class, "string")));
	}

	@Test
	public void testContainsAll0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.containsAll(asList(literal(String.class, "string"))), is(false));
		assertThat(list.containsAll(asList(literal(String.class, "string"), literal(String.class, "other"))), is(false));
	}

	@Test
	public void testContainsAll1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.containsAll(asList(literal(String.class, "string"))), is(true));
		assertThat(list.containsAll(asList(literal(String.class, "string"), literal(String.class, "other"))), is(false));
	}

	@Test
	public void testAddAll() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);

		list.addAll(asList(literal(String.class, "string"), literal(String.class, "other")));

		assertThat(list, contains(literal(String.class, "string"), literal(String.class, "other")));
	}

	@Test
	public void testAddAllAtPos() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);

		list.addAll(asList(literal(String.class, "first"), literal(String.class, "last")));
		list.addAll(1, asList(literal(String.class, "middle"), literal(String.class, "other")));

		assertThat(list, contains(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));
	}

	@Test
	public void testRemoveAll() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));
		list.removeAll(asList(literal(String.class, "middle"), literal(String.class, "other")));

		assertThat(list, contains(
			literal(String.class, "first"),
			literal(String.class, "last")));
	}

	@Test
	public void testRetainAll() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));
		list.retainAll(asList(literal(String.class, "middle"), literal(String.class, "other")));

		assertThat(list, contains(
			literal(String.class, "middle"),
			literal(String.class, "other")));
	}

	@Test
	public void testClear() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));
		list.clear();
		
		assertThat(list, empty());
	}

	@Test
	public void testGet() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));

		assertThat(list.get(0), equalTo(literal(String.class, "first")));
		assertThat(list.get(2), equalTo(literal(String.class, "other")));
	}

	@Test
	public void testSet() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));

		list.set(2, literal(String.class, "changed"));

		assertThat(list.get(0), equalTo(literal(String.class, "first")));
		assertThat(list.get(2), equalTo(literal(String.class, "changed")));
	}

	@Test
	public void testAdd() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));

		list.add(1, literal(String.class, "changed"));

		assertThat(list.get(0), equalTo(literal(String.class, "first")));
		assertThat(list.get(1), equalTo(literal(String.class, "changed")));
		assertThat(list.get(2), equalTo(literal(String.class, "middle")));
	}

	@Test
	public void testIndexOf() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "middle"),
			literal(String.class, "last")));

		assertThat(list.indexOf(literal(String.class, "middle")), equalTo(1));
	}

	@Test
	public void testLastIndexOf() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "middle"),
			literal(String.class, "last")));

		assertThat(list.lastIndexOf(literal(String.class, "middle")), equalTo(2));
	}

	@Test
	public void testListIterator0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.listIterator().hasNext(), is(false));
	}

	@Test
	public void testListIterator1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.listIterator().next(), equalTo(literal(String.class, "string")));
		assertThat(list.listIterator(1).previous(), equalTo(literal(String.class, "string")));
	}

	@Test
	public void testSubList() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.addAll(asList(
			literal(String.class, "first"),
			literal(String.class, "middle"),
			literal(String.class, "other"),
			literal(String.class, "last")));

		assertThat(list.subList(0, 2), equalTo(asList(literal(String.class, "first"), literal(String.class, "middle"))));
		assertThat(list.subList(1, 3), equalTo(asList(literal(String.class, "middle"), literal(String.class, "other"))));
		assertThat(list.subList(2, 4), equalTo(asList(literal(String.class, "other"), literal(String.class, "last"))));
	}

	@Test
	public void testToString0() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.toString(), equalTo("[]"));
	}

	@Test
	public void testToString1() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		list.add(literal(String.class, "string"));
		assertThat(list.toString(), equalTo("[string]"));
	}

	@Test
	public void testAccept() throws Exception {
		SerializedList list = new SerializedList(ArrayList.class).withResult(List.class);
		assertThat(list.accept(new TestValueVisitor()), equalTo("SerializedList"));
	}

}
