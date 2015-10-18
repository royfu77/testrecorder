package com.almondtools.testrecorder.values;

import static com.almondtools.testrecorder.values.SerializedLiteral.isLiteral;
import static com.almondtools.testrecorder.values.SerializedLiteral.literal;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;

public class SerializedLiteralTest {

	@Test
	public void testIsLiteral() throws Exception {
		assertThat(isLiteral(boolean.class), is(true));
		assertThat(isLiteral(Boolean.class), is(true));
		assertThat(isLiteral(int.class), is(true));
		assertThat(isLiteral(Integer.class), is(true));
		assertThat(isLiteral(String.class), is(true));

		assertThat(isLiteral(Object.class), is(false));
		assertThat(isLiteral(BigDecimal.class), is(false));
		assertThat(isLiteral(Collection.class), is(false));
	}

	@Test
	public void testLiteral() throws Exception {
		SerializedLiteral value = literal(String.class, "string");
		SerializedLiteral testvalue = literal(String.class, "string");

		assertThat(testvalue, sameInstance(value));
	}

	@Test
	public void testGetType() throws Exception {
		SerializedLiteral value = literal(String.class, "string");

		assertThat(value.getType(), equalTo(String.class));
	}

	@Test
	public void testGetValue() throws Exception {
		SerializedLiteral value = literal(String.class, "string");

		assertThat(value.getValue(), equalTo("string"));
	}

	@Test
	public void testAccept() throws Exception {
		SerializedLiteral value = literal(String.class, "string");

		assertThat(value.accept(new TestValueVisitor()), equalTo("literal"));
	}

	@Test
	public void testToString0() throws Exception {
		SerializedLiteral value = literal(String.class, "string");

		assertThat(value.toString(), equalTo("string"));
	}

	@Test
	public void testEquals() throws Exception {
		SerializedLiteral value = new SerializedLiteral(String.class, "string");

		SerializedLiteral expected = new SerializedLiteral(String.class, "string");

		assertThat(value, equalTo(expected));
	}

	@Test
	public void testHashCode() throws Exception {
		SerializedLiteral value = new SerializedLiteral(String.class, "string");

		SerializedLiteral expected = new SerializedLiteral(String.class, "string");

		assertThat(value.hashCode(), equalTo(expected.hashCode()));
	}

}
