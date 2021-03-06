package net.amygdalum.testrecorder.deserializers.builder;

import static net.amygdalum.testrecorder.SerializedValues.nullValue;
import static net.amygdalum.testrecorder.TestAgentConfiguration.defaultConfig;
import static net.amygdalum.testrecorder.values.SerializedLiteral.literal;
import static net.amygdalum.testrecorder.values.SerializedNull.nullInstance;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.amygdalum.testrecorder.deserializers.Adaptors;
import net.amygdalum.testrecorder.deserializers.DefaultDeserializerContext;
import net.amygdalum.testrecorder.deserializers.Deserializer;
import net.amygdalum.testrecorder.deserializers.DeserializerTypeManager;
import net.amygdalum.testrecorder.profile.AgentConfiguration;
import net.amygdalum.testrecorder.types.FieldSignature;
import net.amygdalum.testrecorder.types.SerializedField;
import net.amygdalum.testrecorder.types.TypeManager;
import net.amygdalum.testrecorder.util.testobjects.Simple;

public class ConstructorParamTest {

	private AgentConfiguration config;
	private Constructor<Simple> constructor;
	private ConstructorParam constructorParam;

	@BeforeEach
	public void before() throws Exception {
		config = defaultConfig();
		constructor = Simple.class.getDeclaredConstructor(String.class);
		constructorParam = new ConstructorParam(constructor, 0, new SerializedField(new FieldSignature(Simple.class, String.class, "field"), literal("value")), "value");
	}

	@Test
	public void testConstructorParam() throws Exception {
		assertThat(constructorParam.getField().getName()).isEqualTo("field");
		assertThat(constructorParam.getParamNumber()).isEqualTo(0);

		assertThat(constructorParam.computeSerializedValue()).isEqualTo(literal("value"));
		assertThat(constructorParam.getValue()).isEqualTo("value");
	}

	@Test
	public void testConstructorParamWithoutField() throws Exception {
		assertThat(new ConstructorParam(constructor, 0).computeSerializedValue()).isEqualTo(nullValue(null));
		assertThat(new ConstructorParam(constructor, 0).assertType(String.class).computeSerializedValue()).isEqualTo(nullValue(String.class));
		assertThat(new ConstructorParam(constructor, 0).assertType(Object.class).computeSerializedValue()).isEqualTo(nullValue(Object.class));
		assertThat(new ConstructorParam(constructor, 0).assertType(int.class).computeSerializedValue()).isEqualTo(literal(int.class, 0));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(constructorParam.toString()).isEqualTo("public net.amygdalum.testrecorder.util.testobjects.Simple(java.lang.String):0=value=> field");
	}

	@Test
	public void testCompile() throws Exception {
		TypeManager types = new DeserializerTypeManager();

		Deserializer compiler = generator();
		FieldSignature field = new FieldSignature(Simple.class, String.class, "field");
		assertThat(new ConstructorParam(constructor, 0, new SerializedField(field, literal("value")), "value")
			.compile(types, compiler).getValue()).isEqualTo("\"value\"");
		assertThat(new ConstructorParam(constructor, 0, new SerializedField(field, nullInstance()), null)
			.assertType(String.class)
			.compile(types, compiler).getValue()).isEqualTo("null");
		assertThat(new ConstructorParam(constructor, 0)
			.assertType(String.class)
			.compile(types, compiler).getValue()).isEqualTo("null");
		assertThat(new ConstructorParam(constructor, 0)
			.insertTypeCasts()
			.assertType(String.class)
			.compile(types, compiler).getValue()).isEqualTo("(String) null");
		assertThat(new ConstructorParam(constructor, 0, new SerializedField(field, literal("value")), "value")
			.assertType(Integer.class)
			.compile(types, compiler).getValue()).isEqualTo("(Integer) \"value\"");
		assertThat(new ConstructorParam(constructor, 0, new SerializedField(field, literal("value")), "value")
			.assertType(String.class)
			.compile(types, compiler).getValue()).isEqualTo("\"value\"");
	}

	private Deserializer generator() {
		return new SetupGenerators(new Adaptors().load(config.loadConfigurations(SetupGenerator.class))).newGenerator(new DefaultDeserializerContext());
	}

}