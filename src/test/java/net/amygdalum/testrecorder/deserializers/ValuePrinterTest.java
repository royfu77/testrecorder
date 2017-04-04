package net.amygdalum.testrecorder.deserializers;

import static com.almondtools.conmatch.strings.WildcardStringMatcher.containsPattern;
import static net.amygdalum.testrecorder.values.SerializedLiteral.literal;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import net.amygdalum.testrecorder.SerializedImmutableType;
import net.amygdalum.testrecorder.SerializedReferenceType;
import net.amygdalum.testrecorder.values.SerializedArray;
import net.amygdalum.testrecorder.values.SerializedEnum;
import net.amygdalum.testrecorder.values.SerializedField;
import net.amygdalum.testrecorder.values.SerializedImmutable;
import net.amygdalum.testrecorder.values.SerializedList;
import net.amygdalum.testrecorder.values.SerializedLiteral;
import net.amygdalum.testrecorder.values.SerializedMap;
import net.amygdalum.testrecorder.values.SerializedNull;
import net.amygdalum.testrecorder.values.SerializedObject;
import net.amygdalum.testrecorder.values.SerializedSet;

public class ValuePrinterTest {

    private ValuePrinter printer;

    @Before
    public void before() throws Exception {
        printer = new ValuePrinter();
    }

    @Test
    public void testVisitField() throws Exception {
        SerializedField field = new SerializedField(TestObject.class, "field", String.class, literal("v"));

        assertThat(printer.visitField(field), equalTo("java.lang.String field: v"));
    }

    @Test
    public void testVisitObject() throws Exception {
        SerializedObject object = new SerializedObject(TestObject.class);
        object.addField(new SerializedField(TestObject.class, "field", String.class, literal("v")));

        String visitReferenceType = printer.visitReferenceType(object);

        assertThat(visitReferenceType, containsPattern(""
            + "net.amygdalum.testrecorder.deserializers.ValuePrinterTest$TestObject/*{*"
            + "java.lang.String field: v*"
            + "}"));
    }

    @Test
    public void testVisitObjectCached() throws Exception {
        SerializedObject object = new SerializedObject(TestObject.class);
        object.addField(new SerializedField(TestObject.class, "field", String.class, literal("v")));

        String visitReferenceType = printer.visitReferenceType(object);
        visitReferenceType = printer.visitReferenceType(object);

        assertThat(visitReferenceType, containsPattern("net.amygdalum.testrecorder.deserializers.ValuePrinterTest$TestObject/*"));
        assertThat(visitReferenceType, not(containsPattern("java.lang.String field: v")));
    }

    @Test
    public void testVisitArray() throws Exception {
        SerializedArray object = new SerializedArray(int[].class);
        object.add(literal(22));

        String visitReferenceType = printer.visitReferenceType(object);

        assertThat(visitReferenceType, equalTo("<22>"));
    }

    @Test
    public void testVisitList() throws Exception {
        SerializedList object = new SerializedList(ArrayList.class);
        object.add(literal(1));

        String visitReferenceType = printer.visitReferenceType(object);

        assertThat(visitReferenceType, equalTo("[1]"));
    }

    @Test
    public void testVisitSet() throws Exception {
        SerializedSet object = new SerializedSet(HashSet.class);
        object.add(literal(true));

        String visitReferenceType = printer.visitReferenceType(object);

        assertThat(visitReferenceType, equalTo("{true}"));
    }

    @Test
    public void testVisitMap() throws Exception {
        SerializedMap object = new SerializedMap(HashMap.class);
        object.put(literal(1.0), literal("one"));

        String visitReferenceType = printer.visitReferenceType(object);

        assertThat(visitReferenceType, equalTo("{1.0:one}"));
    }

    @Test
    public void testVisitNull() throws Exception {
        SerializedReferenceType object = SerializedNull.nullInstance(Object.class);
        assertThat(printer.visitReferenceType(object), equalTo("null"));
    }

    @Test
    public void testVisitOtherReferenceType() throws Exception {
        SerializedReferenceType object = Mockito.mock(SerializedReferenceType.class);
        assertThat(printer.visitReferenceType(object), equalTo(""));
    }

    @Test
    public void testVisitImmutable() throws Exception {
        SerializedImmutable<BigDecimal> serializedImmutable = new SerializedImmutable<>(BigDecimal.class);
        serializedImmutable.setValue(new BigDecimal("2.0"));

        String visitImmutableType = printer.visitImmutableType(serializedImmutable);

        assertThat(visitImmutableType, equalTo("2.0"));
    }

    @Test
    public void testVisitEnum() throws Exception {
        SerializedEnum serializedEnum = new SerializedEnum(TestEnum.class);
        serializedEnum.setName("ENUM");

        String visitImmutableType = printer.visitImmutableType(serializedEnum);

        assertThat(visitImmutableType, equalTo("ENUM"));
    }

    @Test
    public void testVisitOtherImmutable() throws Exception {
        SerializedImmutableType serializedImmutable = Mockito.mock(SerializedImmutableType.class);

        String visitImmutableType = printer.visitImmutableType(serializedImmutable);

        assertThat(visitImmutableType, equalTo(""));
    }

    @Test
    public void testVisitValueType() throws Exception {
        SerializedLiteral serializedLiteral = SerializedLiteral.literal('a');

        String visitImmutableType = printer.visitValueType(serializedLiteral);

        assertThat(visitImmutableType, equalTo("a"));
    }

    @Test
    public void testVisitValueTypeCached() throws Exception {
        SerializedLiteral serializedLiteral = SerializedLiteral.literal('a');

        String visitImmutableType = printer.visitValueType(serializedLiteral);
        visitImmutableType = printer.visitValueType(serializedLiteral);

        assertThat(visitImmutableType, equalTo("a"));
    }

    public static enum TestEnum {
        ENUM;
    }

    public static class TestObject {

        private String field;

        public void setField(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

}