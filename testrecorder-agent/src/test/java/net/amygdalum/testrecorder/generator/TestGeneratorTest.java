package net.amygdalum.testrecorder.generator;

import static net.amygdalum.extensions.assertj.Assertions.assertThat;
import static net.amygdalum.testrecorder.TestAgentConfiguration.defaultConfig;
import static net.amygdalum.testrecorder.values.SerializedLiteral.literal;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.amygdalum.testrecorder.ClassDescriptor;
import net.amygdalum.testrecorder.SnapshotManager;
import net.amygdalum.testrecorder.TestAgentConfiguration;
import net.amygdalum.testrecorder.types.ContextSnapshot;
import net.amygdalum.testrecorder.types.FieldSignature;
import net.amygdalum.testrecorder.types.MethodSignature;
import net.amygdalum.testrecorder.types.SerializedField;
import net.amygdalum.testrecorder.types.SerializedInput;
import net.amygdalum.testrecorder.types.SerializedOutput;
import net.amygdalum.testrecorder.types.VirtualMethodSignature;
import net.amygdalum.testrecorder.util.ExtensibleClassLoader;
import net.amygdalum.testrecorder.util.TemporaryFolder;
import net.amygdalum.testrecorder.util.TemporaryFolderExtension;
import net.amygdalum.testrecorder.values.SerializedObject;

@ExtendWith(TemporaryFolderExtension.class)
public class TestGeneratorTest {

	private static SnapshotManager saveManager;

	private ExtensibleClassLoader loader;
	private TestAgentConfiguration config;
	private TestGenerator testGenerator;

	@BeforeAll
	static void beforeClass() throws Exception {
		saveManager = SnapshotManager.MANAGER;
	}

	@AfterAll
	static void afterClass() throws Exception {
		SnapshotManager.MANAGER = saveManager;
	}

	@BeforeEach
	void before() throws Exception {
		loader = new ExtensibleClassLoader(TestGenerator.class.getClassLoader());
		config = defaultConfig().withLoader(loader);
		testGenerator = new TestGenerator(config);
	}

	@Test
	void testAccept() throws Exception {
		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot.setSetupArgs(literal(int.class, 16));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot.setExpectArgs(literal(int.class, 16));
		snapshot.setExpectResult(literal(int.class, 22));
		snapshot.setExpectGlobals(new SerializedField[0]);

		testGenerator.accept(snapshot);

		testGenerator.await();
		assertThat(testGenerator.testsFor(TestGeneratorTest.class))
			.hasSize(1)
			.anySatisfy(test -> {
				assertThat(test).containsSubsequence("int field = 12;",
					"intMethod(16);",
					"equalTo(22)",
					"int field = 8;");
			});
	}

	@Test
	void testAcceptWithInput() throws Exception {
		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot.setSetupArgs(literal(int.class, 16));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot.setExpectArgs(literal(int.class, 16));
		snapshot.setExpectResult(literal(int.class, 22));
		snapshot.setExpectGlobals(new SerializedField[0]);
		snapshot.addInput(new SerializedInput(42, new MethodSignature(System.class, long.class, "currentTimeMillis", new Type[0])).updateResult(literal(42l)));

		testGenerator.accept(snapshot);

		testGenerator.await();
		assertThat(testGenerator.renderTest(TestGeneratorTest.class).getTestCode())
			.containsSubsequence(
				"@Before",
				"@After",
				"public void resetFakeIO() throws Exception {",
				"FakeIO.reset();",
				"}");
	}

	@Test
	void testAcceptWithOutput() throws Exception {
		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot.setSetupArgs(literal(int.class, 16));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot.setExpectArgs(literal(int.class, 16));
		snapshot.setExpectResult(literal(int.class, 22));
		snapshot.setExpectGlobals(new SerializedField[0]);
		snapshot.addOutput(new SerializedOutput(42, new MethodSignature(Writer.class, void.class, "write", new Type[] { Writer.class })).updateArguments(literal("hello")));

		testGenerator.accept(snapshot);

		testGenerator.await();
		assertThat(testGenerator.renderTest(TestGeneratorTest.class).getTestCode())
			.containsSubsequence(
				"@Before",
				"@After",
				"public void resetFakeIO() throws Exception {",
				"FakeIO.reset();",
				"}");
	}

	@Test
	void testSuppressesWarnings() throws Exception {
		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot.setSetupArgs(literal(int.class, 16));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot.setExpectArgs(literal(int.class, 16));
		snapshot.setExpectResult(literal(int.class, 22));
		snapshot.setExpectGlobals(new SerializedField[0]);

		testGenerator.accept(snapshot);

		testGenerator.await();
		assertThat(testGenerator.renderTest(MyClass.class).getTestCode()).containsSubsequence("@SuppressWarnings(\"unused\")" + System.lineSeparator() + "public class");
	}

	@Test
	void testTestsForEmpty() throws Exception {
		assertThat(testGenerator.testsFor(MyClass.class)).isEmpty();
	}

	@Test
	void testTestsForAfterClear() throws Exception {
		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot.setSetupArgs(literal(int.class, 16));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot.setExpectArgs(literal(int.class, 16));
		snapshot.setExpectResult(literal(int.class, 22));
		snapshot.setExpectGlobals(new SerializedField[0]);
		testGenerator.accept(snapshot);

		testGenerator.clearResults();

		assertThat(testGenerator.testsFor(MyClass.class)).isEmpty();
	}

	@Test
	void testRenderCode() throws Exception {
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		ContextSnapshot snapshot1 = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		snapshot1.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot1.setSetupArgs(literal(int.class, 16));
		snapshot1.setSetupGlobals(new SerializedField[0]);
		snapshot1.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot1.setExpectArgs(literal(int.class, 16));
		snapshot1.setExpectResult(literal(int.class, 22));
		snapshot1.setExpectGlobals(new SerializedField[0]);
		ContextSnapshot snapshot2 = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		snapshot2.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 13))));
		snapshot2.setSetupArgs(literal(int.class, 17));
		snapshot2.setSetupGlobals(new SerializedField[0]);
		snapshot2.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 9))));
		snapshot2.setExpectArgs(literal(int.class, 17));
		snapshot2.setExpectResult(literal(int.class, 23));
		snapshot2.setExpectGlobals(new SerializedField[0]);

		testGenerator.accept(snapshot1);
		testGenerator.accept(snapshot2);

		testGenerator.await();
		assertThat(testGenerator.renderTest(TestGeneratorTest.class).getTestCode()).containsSubsequence(
			"int field = 12;",
			"intMethod(16);",
			"equalTo(22)",
			"int field = 8;",
			"int field = 13;",
			"intMethod(17);",
			"equalTo(23)",
			"int field = 9;");
	}

	@Test
	void testComputeClassName() throws Exception {
		assertThat(testGenerator.computeClassName(ClassDescriptor.of(MyClass.class))).isEqualTo("MyClassRecordedTest");
	}

	@Test
	void testWriteResults(TemporaryFolder folder) throws Exception {
		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
		snapshot.setSetupArgs(literal(int.class, 16));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
		snapshot.setExpectArgs(literal(int.class, 16));
		snapshot.setExpectResult(literal(int.class, 22));
		snapshot.setExpectGlobals(new SerializedField[0]);

		testGenerator.accept(snapshot);

		testGenerator.await();
		testGenerator.writeResults(folder.getRoot());

		assertThat(Files.exists(folder.resolve("net/amygdalum/testrecorder/generator/TestGeneratorTestRecordedTest.java"))).isTrue();
	}

	private ContextSnapshot contextSnapshot(Class<?> declaringClass, Type resultType, String methodName, Type... argumentTypes) {
		return new ContextSnapshot(0, "key", new VirtualMethodSignature(new MethodSignature(declaringClass, resultType, methodName, argumentTypes)));
	}

	private SerializedObject objectOf(Class<MyClass> type, SerializedField... fields) {
		SerializedObject setupThis = new SerializedObject(type);
		for (SerializedField field : fields) {
			setupThis.addField(field);
		}
		return setupThis;
	}

	@SuppressWarnings("unused")
	private static class MyClass {

		private int field;

		public int intMethod(int arg) {
			return field + arg;
		}
	}

}
