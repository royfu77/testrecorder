package net.amygdalum.testrecorder.types;

import static net.amygdalum.extensions.assertj.conventions.DefaultEquality.defaultEquality;
import static net.amygdalum.testrecorder.values.SerializedLiteral.literal;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.amygdalum.testrecorder.values.SerializedLiteral;
import net.amygdalum.testrecorder.values.SerializedNull;
import net.amygdalum.testrecorder.values.SerializedObject;

public class SerializedOutputTest {

	private static final SerializedLiteral STRING_LITERAL = literal("Hello");
	private static final SerializedObject PRINTSTREAM_OBJECT = new SerializedObject(PrintStream.class);
	private static final SerializedValue VOID = SerializedNull.VOID;

	private SerializedOutput output;
	private SerializedOutput outputNoResult;

	@BeforeEach
	void before() throws Exception {
		output = new SerializedOutput(41, PrintStream.class, "append", PrintStream.class, new Type[] { CharSequence.class })
			.updateArguments(STRING_LITERAL)
			.updateResult(PRINTSTREAM_OBJECT);
		outputNoResult = new SerializedOutput(41, PrintStream.class, "println", void.class, new Type[] { String.class })
			.updateArguments(STRING_LITERAL)
			.updateResult(VOID);
	}

	@Test
	void testGetId() throws Exception {
		assertThat(output.getId()).isEqualTo(41);
	}

	@Test
	void testGetDeclaringClass() throws Exception {
		assertThat(output.getDeclaringClass()).isSameAs(PrintStream.class);
	}

	@Test
	void testGetName() throws Exception {
		assertThat(output.getName()).isSameAs("append");
		assertThat(outputNoResult.getName()).isSameAs("println");
	}

	@Test
	void testGetTypes() throws Exception {
		assertThat(output.getTypes()).containsExactly(CharSequence.class);
		assertThat(outputNoResult.getTypes()).containsExactly(String.class);
	}

	@Test
	void testGetArguments() throws Exception {
		assertThat(output.getArguments()).extracting(SerializedArgument::getValue).containsExactly(STRING_LITERAL);
	}

	@Test
	void testGetResultType() throws Exception {
		assertThat(output.getResultType()).isSameAs(PrintStream.class);
		assertThat(outputNoResult.getResultType()).isSameAs(void.class);
	}

	@Test
	void testGetResult() throws Exception {
		assertThat(output.getResult().getValue()).isInstanceOf(SerializedObject.class);
		assertThat(outputNoResult.getResult().getValue()).isSameAs(VOID);
	}

	@Test
	void testEquals() throws Exception {
		assertThat(outputNoResult).satisfies(defaultEquality()
			.andEqualTo(new SerializedOutput(41, PrintStream.class, "println", void.class, new Type[] { String.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(VOID))
			.andNotEqualTo(output)
			.andNotEqualTo(new SerializedOutput(41, PrintStream.class, "println", void.class, new Type[] { String.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(null))
			.andNotEqualTo(new SerializedOutput(42, PrintStream.class, "println", void.class, new Type[] { String.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(VOID))
			.andNotEqualTo(new SerializedOutput(41, PrintWriter.class, "println", void.class, new Type[] { String.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(VOID))
			.andNotEqualTo(new SerializedOutput(41, PrintStream.class, "print", void.class, new Type[] { String.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(VOID))
			.andNotEqualTo(new SerializedOutput(41, PrintStream.class, "println", void.class, new Type[] { Object.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(VOID))
			.andNotEqualTo(new SerializedOutput(41, PrintStream.class, "println", void.class, new Type[] { String.class })
				.updateArguments(literal("Hello World"))
				.updateResult(VOID))
			.conventions());

		assertThat(output).satisfies(defaultEquality()
			.andEqualTo(new SerializedOutput(41, PrintStream.class, "append", PrintStream.class, new Type[] { CharSequence.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(PRINTSTREAM_OBJECT))
			.andNotEqualTo(outputNoResult)
			.andNotEqualTo(new SerializedOutput(41, PrintStream.class, "append", PrintStream.class, new Type[] { CharSequence.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(null))
			.andNotEqualTo(new SerializedOutput(41, PrintStream.class, "append", OutputStream.class, new Type[] { CharSequence.class })
				.updateArguments(STRING_LITERAL)
				.updateResult(VOID))
			.conventions());
	}

	@Test
	void testToString() throws Exception {
		assertThat(output.toString()).contains("PrintStream", "append", "Hello");

		assertThat(outputNoResult.toString()).contains("PrintStream", "println", "Hello");
	}

	@Test
	void testUpdateArguments() throws Exception {
		output.updateArguments((SerializedValue[]) null);
		
		assertThat(output.getArguments()).isEmpty();
	}

	@Test
	void testIsComplete() {
		assertThat(output.isComplete()).isTrue();
		assertThat(outputNoResult.isComplete()).isTrue();
	}
	
	@Test
	void testIsCompleteOnMissingResult() throws Exception {
		output.result = null;
		assertThat(output.isComplete()).isFalse();
	}
	
	@Test
	void testIsCompleteOnMissingResultType() throws Exception {
		output.resultType = null;
		assertThat(output.isComplete()).isFalse();
	}
	
	@Test
	void testIsCompleteOnMissingArgumentType() throws Exception {
		output.types = null;
		assertThat(output.isComplete()).isFalse();
	}
	
	@Test
	void testIsCompleteOnNullArguments() throws Exception {
		output.arguments = null;
		assertThat(output.isComplete()).isFalse();
	}
	
	@Test
	void testIsCompleteOnMissingArguments() throws Exception {
		output.arguments = new SerializedArgument[0];
		assertThat(output.isComplete()).isFalse();
	}
	
	@Test
	void testHasResult() throws Exception {
		assertThat(output.hasResult()).isTrue();
		assertThat(outputNoResult.hasResult()).isFalse();
	}
	
	@Test
	void testHasResultWithNoResultType() throws Exception {
		output.resultType = null;
		assertThat(output.hasResult()).isFalse();
	}
	
	@Test
	void testHasResultWithNoResult() throws Exception {
		output.result = null;
		assertThat(output.hasResult()).isFalse();
	}
	
}