package net.amygdalum.testrecorder.generator;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static net.amygdalum.extensions.assertj.Assertions.assertThat;
import static net.amygdalum.testrecorder.TestAgentConfiguration.defaultConfig;
import static net.amygdalum.testrecorder.values.SerializedLiteral.literal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.amygdalum.testrecorder.SnapshotManager;
import net.amygdalum.testrecorder.TestAgentConfiguration;
import net.amygdalum.testrecorder.deserializers.CustomAnnotation;
import net.amygdalum.testrecorder.profile.PerformanceProfile;
import net.amygdalum.testrecorder.types.ContextSnapshot;
import net.amygdalum.testrecorder.types.FieldSignature;
import net.amygdalum.testrecorder.types.MethodSignature;
import net.amygdalum.testrecorder.types.SerializedField;
import net.amygdalum.testrecorder.types.SerializedInput;
import net.amygdalum.testrecorder.types.SerializedOutput;
import net.amygdalum.testrecorder.types.TypeManager;
import net.amygdalum.testrecorder.types.VirtualMethodSignature;
import net.amygdalum.testrecorder.util.ClassDescriptor;
import net.amygdalum.testrecorder.util.ExtensibleClassLoader;
import net.amygdalum.testrecorder.util.LogLevel;
import net.amygdalum.testrecorder.util.LoggerExtension;
import net.amygdalum.testrecorder.util.TemporaryFolder;
import net.amygdalum.testrecorder.util.TemporaryFolderExtension;
import net.amygdalum.testrecorder.values.SerializedObject;
import net.amygdalum.xrayinterface.XRayInterface;

@ExtendWith(TemporaryFolderExtension.class)
public class ScheduledTestGeneratorTest {

	private static SnapshotManager saveManager;

	private ExtensibleClassLoader loader;
	private TestAgentConfiguration config;
	private ScheduledTestGenerator testGenerator;

	@BeforeAll
	static void beforeClass() throws Exception {
		saveManager = SnapshotManager.MANAGER;
	}

	@AfterAll
	static void afterClass() throws Exception {
		SnapshotManager.MANAGER = saveManager;
		shutdownHooks().entrySet().stream()
			.filter(e -> e.getKey().getName().equals("$generate-shutdown"))
			.map(e -> e.getValue())
			.findFirst()
			.ifPresent(shutdown -> {
				Runtime.getRuntime().removeShutdownHook(shutdown);
				XRayInterface.xray(ScheduledTestGenerator.class).to(StaticScheduledTestGenerator.class).setDumpOnShutDown(null);
			});
	}

	@BeforeEach
	void before() throws Exception {
		XRayInterface.xray(ScheduledTestGenerator.class).to(StaticScheduledTestGenerator.class).setDumpOnShutDown(null);
		loader = new ExtensibleClassLoader(ScheduledTestGenerator.class.getClassLoader());
		config = defaultConfig().withLoader(loader);
		testGenerator = new ScheduledTestGenerator(config);
		testGenerator.counterMaximum = 1;
	}

	@Test
	void testTestGenerator() throws Exception {
		loader.defineResource("agentconfig/net.amygdalum.testrecorder.generator.TestGeneratorProfile", "net.amygdalum.testrecorder.generator.ScheduledTestGeneratorTest$InvalidTestTemplateTestGeneratorProfile".getBytes());
		config.reset();
		assertThatCode(() -> new ScheduledTestGenerator(config))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testReload() throws Exception {
		loader.defineResource("agentconfig/net.amygdalum.testrecorder.profile.PerformanceProfile", "net.amygdalum.testrecorder.generator.ScheduledTestGeneratorTest$CustomPerformanceProfile".getBytes());
		loader.defineResource("agentconfig/net.amygdalum.testrecorder.generator.TestGeneratorProfile", "net.amygdalum.testrecorder.generator.ScheduledTestGeneratorTest$Profile".getBytes());
		loader.defineResource("agentconfig/net.amygdalum.testrecorder.deserializers.builder.SetupGenerator", "".getBytes());
		loader.defineResource("agentconfig/net.amygdalum.testrecorder.deserializers.matcher.MatcherGenerator", "".getBytes());
		config.reset();

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
		snapshot.addOutput(new SerializedOutput(42, new MethodSignature(Writer.class, void.class, "write", new Type[] {Writer.class})).updateArguments(literal("hello")));

		testGenerator.reload(config);

		ClassGenerator gen = testGenerator.generatorFor(ClassDescriptor.of(MyClass.class));
		gen.generate(snapshot);
		assertThat(gen.render()).containsWildcardPattern("Test*resetFakeIO:setup*test");
	}

	@Nested
	class testAccept {
		@Test
		void onCommon() throws Exception {
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
			assertThat(testGenerator.testsFor(ScheduledTestGeneratorTest.class))
				.hasSize(1)
				.anySatisfy(test -> {
					assertThat(test)
						.containsSubsequence(
							"int field = 12;",
							"intMethod(16);",
							"equalTo(22)",
							"int field = 8;");
				});
		}

		@Test
		void withInput() throws Exception {
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
			assertThat(testGenerator.renderTest(ScheduledTestGeneratorTest.class).getTestCode())
				.containsSubsequence(
					"@Before",
					"@After",
					"public void resetFakeIO() throws Exception {",
					"FakeIO.reset();",
					"}");
		}

		@Test
		void withOutput() throws Exception {
			ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
			FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
			snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 12))));
			snapshot.setSetupArgs(literal(int.class, 16));
			snapshot.setSetupGlobals(new SerializedField[0]);
			snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, 8))));
			snapshot.setExpectArgs(literal(int.class, 16));
			snapshot.setExpectResult(literal(int.class, 22));
			snapshot.setExpectGlobals(new SerializedField[0]);
			snapshot.addOutput(new SerializedOutput(42, new MethodSignature(Writer.class, void.class, "write", new Type[] {Writer.class})).updateArguments(literal("hello")));

			testGenerator.accept(snapshot);

			testGenerator.await();
			assertThat(testGenerator.renderTest(ScheduledTestGeneratorTest.class).getTestCode())
				.containsSubsequence(
					"@Before",
					"@After",
					"public void resetFakeIO() throws Exception {",
					"FakeIO.reset();",
					"}");
		}

		@Test
		void suppressinWarnings() throws Exception {
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
		
		@ExtendWith(LoggerExtension.class)
		@Test
		void withExceptionIsLogging(@LogLevel("error") ByteArrayOutputStream error) throws Exception {
			ContextSnapshot snapshot = new ContextSnapshot(0, "key", new VirtualMethodSignature(new MethodSignature(String.class, String.class, "toString", new Class[0]))) {
				@Override
				public Type getThisType() {
					throw new RuntimeException("Message for RuntimeException");
				}
			};
			snapshot.setSetupThis(literal(String.class, "astring"));
			snapshot.setSetupArgs();
			snapshot.setSetupGlobals(new SerializedField[0]);
			snapshot.setExpectThis(literal(String.class, "astring"));
			snapshot.setExpectArgs();
			snapshot.setExpectResult(literal(String.class, "astring"));
			snapshot.setExpectGlobals(new SerializedField[0]);

			testGenerator.accept(snapshot);
			testGenerator.await();
			assertThat(error.toString()).contains("Message for RuntimeException");
		}

	}

	@Nested
	class testTestsFor {
		@Test
		void onEmpty() throws Exception {
			assertThat(testGenerator.testsFor(MyClass.class)).isEmpty();
		}

		@Test
		void afterClear() throws Exception {
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
	}

	@Test
	void testRenderTest() throws Exception {
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

		testGenerator.counterMaximum = 2;
		testGenerator.accept(snapshot1);
		testGenerator.accept(snapshot2);

		testGenerator.await();
		assertThat(testGenerator.renderTest(ScheduledTestGeneratorTest.class).getTestCode()).containsSubsequence(
			"int field = 12;",
			"intMethod(16);",
			"equalTo(22)",
			"int field = 8;",
			"int field = 13;",
			"intMethod(17);",
			"equalTo(23)",
			"int field = 9;");
	}

	@Nested
	class testComputeClassName {
		@Test
		void onCommon() throws Exception {
			assertThat(testGenerator.computeClassName(ClassDescriptor.of(MyClass.class))).isEqualTo("MyClassRecordedTest");
		}

		@Test
		void withTemplateClass() throws Exception {
			testGenerator.classNameTemplate = "${class}Suffix";
			assertThat(testGenerator.computeClassName(ClassDescriptor.of(MyClass.class))).isEqualTo("MyClassSuffix");
		}

		@Test
		void withTemplateCounter() throws Exception {
			testGenerator.classNameTemplate = "${counter}Suffix";
			assertThat(testGenerator.computeClassName(ClassDescriptor.of(MyClass.class))).isEqualTo("0Suffix");
		}

		@Test
		void withTemplateMillis() throws Exception {
			testGenerator.classNameTemplate = "Prefix${millis}Suffix";
			assertThat(testGenerator.computeClassName(ClassDescriptor.of(MyClass.class))).containsWildcardPattern("Prefix*Suffix");
		}
	}

	@Nested
	class testWriteResults {
		@Test
		void withOrdinaryPackage(TemporaryFolder folder) throws Exception {
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

			assertThat(Files.exists(folder.resolve("net/amygdalum/testrecorder/generator/ScheduledTestGeneratorTestRecordedTest.java"))).isTrue();
		}

		@Test
		void withProtectedPackage(TemporaryFolder folder) throws Exception {
			ContextSnapshot snapshot = contextSnapshot(String.class, String.class, "toString");
			snapshot.setSetupThis(literal(String.class, "astring"));
			snapshot.setSetupArgs();
			snapshot.setSetupGlobals(new SerializedField[0]);
			snapshot.setExpectThis(literal(String.class, "astring"));
			snapshot.setExpectArgs();
			snapshot.setExpectResult(literal(String.class, "astring"));
			snapshot.setExpectGlobals(new SerializedField[0]);

			testGenerator.accept(snapshot);

			testGenerator.await();
			testGenerator.writeResults(folder.getRoot());

			assertThat(Files.exists(folder.resolve("test/java/lang/StringRecordedTest.java"))).isTrue();
		}
	}

	@Test
	void testWithDumpOnTimeInterval(TemporaryFolder folder) throws Exception {
		testGenerator.counterMaximum = 5;
		testGenerator.classNameTemplate = "${counter}Test";
		testGenerator.timeInterval = 1000;
		testGenerator.generateTo = folder.getRoot();

		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).isEmpty();
		Thread.sleep(1000);
		testGenerator.accept(newSnapshot());
		testGenerator.await();

		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java");

		testGenerator.accept(newSnapshot());
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java");
		Thread.sleep(1000);
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java", "2Test.java");
	}

	@Test
	void testWithDumpOnCounterInterval(TemporaryFolder folder) throws Exception {
		testGenerator.counterMaximum = 5;
		testGenerator.generateTo = folder.getRoot();
		testGenerator.classNameTemplate = "${counter}Test";
		testGenerator.counterInterval = 2;

		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).isEmpty();
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java");
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java");
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java", "2Test.java");
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java", "2Test.java");
		testGenerator.accept(newSnapshot());
		testGenerator.await();
		assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java", "2Test.java");
	}

	@Nested
	class testWithDumpOnShutDown {
		@Test
		void singleThread(TemporaryFolder folder) throws Exception {
			testGenerator.counterMaximum = 5;
			testGenerator.generateTo = folder.getRoot();
			testGenerator.classNameTemplate = "${counter}Test";
			testGenerator.dumpOnShutdown(true);

			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.await();
			assertThat(folder.fileNames()).isEmpty();

			Thread shutdown = shutdownHooks().entrySet().stream()
				.filter(e -> e.getKey().getName().equals("$generate-shutdown"))
				.map(e -> e.getValue())
				.findFirst().orElseThrow(() -> new AssertionError("no shutdown thread"));

			shutdown.run();
			shutdown.join();

			assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java");
		}

		@Test
		void concurrent(TemporaryFolder folder) throws Exception {
			testGenerator.counterMaximum = 2;
			testGenerator.generateTo = folder.getRoot();
			testGenerator.classNameTemplate = "${counter}Test";
			testGenerator.dumpOnShutdown(true);

			ScheduledTestGenerator second = new ScheduledTestGenerator(config);
			second.counterMaximum = 2;
			second.generateTo = folder.getRoot();
			second.classNameTemplate = "${counter}SecondTest";
			second.dumpOnShutdown(true);

			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			testGenerator.accept(newSnapshot());
			second.accept(newSnapshot());
			second.accept(newSnapshot());
			second.accept(newSnapshot());
			assertThat(folder.fileNames()).isEmpty();

			Thread shutdown = shutdownHooks().entrySet().stream()
				.filter(e -> e.getKey().getName().equals("$generate-shutdown"))
				.map(e -> e.getValue())
				.findFirst().orElseThrow(() -> new AssertionError("no shutdown thread"));

			shutdown.run();
			shutdown.join();

			assertThat(folder.fileNames()).containsExactlyInAnyOrder("0Test.java", "0SecondTest.java");
		}
	}

	private ContextSnapshot contextSnapshot(Class<?> declaringClass, Type resultType, String methodName, Type... argumentTypes) {
		return new ContextSnapshot(0, "key", new VirtualMethodSignature(new MethodSignature(declaringClass, resultType, methodName, argumentTypes)));
	}

	private static int base = 8;

	private ContextSnapshot newSnapshot() {
		base++;

		ContextSnapshot snapshot = contextSnapshot(MyClass.class, int.class, "intMethod", int.class);
		FieldSignature field = new FieldSignature(MyClass.class, int.class, "field");
		snapshot.setSetupThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, base + 4))));
		snapshot.setSetupArgs(literal(int.class, base + 8));
		snapshot.setSetupGlobals(new SerializedField[0]);
		snapshot.setExpectThis(objectOf(MyClass.class, new SerializedField(field, literal(int.class, base))));
		snapshot.setExpectArgs(literal(int.class, base + 8));
		snapshot.setExpectResult(literal(int.class, base + 14));
		snapshot.setExpectGlobals(new SerializedField[0]);
		return snapshot;
	}

	private SerializedObject objectOf(Class<MyClass> type, SerializedField... fields) {
		SerializedObject setupThis = new SerializedObject(type);
		for (SerializedField field : fields) {
			setupThis.addField(field);
		}
		return setupThis;
	}

	private static Map<Thread, Thread> shutdownHooks() throws ClassNotFoundException {
		StaticShutdownHooks staticShutdownHooks = XRayInterface.xray(Class.forName("java.lang.ApplicationShutdownHooks"))
			.to(StaticShutdownHooks.class);

		return staticShutdownHooks.getHooks();
	}

	@SuppressWarnings("unused")
	private static class MyClass {

		private int field;

		public int intMethod(int arg) {
			return field + arg;
		}
	}

	public static class CustomPerformanceProfile implements PerformanceProfile {

		@Override
		public long getTimeoutInMillis() {
			return 0;
		}

		@Override
		public long getIdleTime() {
			return 0;
		}
	}

	public static class Profile implements TestGeneratorProfile {

		@Override
		public List<CustomAnnotation> annotations() {
			return emptyList();
		}

		@Override
		public Class<? extends TestTemplate> template() {
			return CustomTemplate.class;
		}

	}

	public static class CustomTemplate implements TestTemplate {

		@Override
		public Class<?>[] getTypes() {
			return new Class[0];
		}

		@Override
		public String testClass(String methodName, TypeManager types, Map<String, String> setups, Set<String> tests) {
			return "Test\n"
				+ setups.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(joining("\n", "\n", "\n"))
				+ tests.stream().collect(joining("\n", "\n", "\n"));
		}

		@Override
		public String setupMethod(String methodName, TypeManager types, List<String> annotations, List<String> statements) {
			return "setup";
		}

		@Override
		public String testMethod(String methodName, TypeManager types, List<String> annotations, List<String> statements) {
			return "test";
		}

	}

	interface StaticShutdownHooks {
		IdentityHashMap<Thread, Thread> getHooks();
	}

	interface StaticScheduledTestGenerator {
		void setDumpOnShutDown(Set<ScheduledTestGenerator> value);
	}

	public static class InvalidTestTemplateTestGeneratorProfile implements TestGeneratorProfile {

		@Override
		public List<CustomAnnotation> annotations() {
			return emptyList();
		}

		@Override
		public Class<? extends TestTemplate> template() {
			return InvalidTestTemplate.class;
		}

	}

	public static class InvalidTestTemplate implements TestTemplate {

		InvalidTestTemplate(String s) {
			//constructor with arguments to suppress auto constructor
		}

		@Override
		public Class<?>[] getTypes() {
			return new Class[0];
		}

		@Override
		public String testClass(String methodName, TypeManager types, Map<String, String> setups, Set<String> tests) {
			return "class";
		}

		@Override
		public String setupMethod(String methodName, TypeManager types, List<String> annotations, List<String> statements) {
			return "setup";
		}

		@Override
		public String testMethod(String methodName, TypeManager types, List<String> annotations, List<String> statements) {
			return "test";
		}

	}

}
