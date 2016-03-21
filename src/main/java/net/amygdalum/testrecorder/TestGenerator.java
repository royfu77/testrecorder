package net.amygdalum.testrecorder;

import static java.lang.Character.toUpperCase;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;
import static net.amygdalum.testrecorder.SnapshotInstrumentor.SNAPSHOT_GENERATOR_FIELD_NAME;
import static net.amygdalum.testrecorder.visitors.Templates.assignFieldStatement;
import static net.amygdalum.testrecorder.visitors.Templates.assignLocalVariableStatement;
import static net.amygdalum.testrecorder.visitors.Templates.callLocalMethod;
import static net.amygdalum.testrecorder.visitors.Templates.callLocalMethodStatement;
import static net.amygdalum.testrecorder.visitors.Templates.callMethod;
import static net.amygdalum.testrecorder.visitors.Templates.callMethodChainStatement;
import static net.amygdalum.testrecorder.visitors.Templates.callMethodStatement;
import static net.amygdalum.testrecorder.visitors.Templates.captureException;
import static net.amygdalum.testrecorder.visitors.Templates.classOf;
import static net.amygdalum.testrecorder.visitors.Templates.expressionStatement;
import static net.amygdalum.testrecorder.visitors.Templates.fieldAccess;
import static net.amygdalum.testrecorder.visitors.Templates.fieldDeclaration;
import static net.amygdalum.testrecorder.visitors.Templates.newObject;
import static net.amygdalum.testrecorder.visitors.Templates.stringOf;
import static net.amygdalum.testrecorder.visitors.TypeManager.getBase;
import static net.amygdalum.testrecorder.visitors.TypeManager.isPrimitive;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stringtemplate.v4.ST;

import com.almondtools.conmatch.exceptions.Exceptions;

import net.amygdalum.testrecorder.util.ExpectedOutput;
import net.amygdalum.testrecorder.util.IORecorder;
import net.amygdalum.testrecorder.util.RecordInput;
import net.amygdalum.testrecorder.util.RecordOutput;
import net.amygdalum.testrecorder.util.SetupInput;
import net.amygdalum.testrecorder.values.SerializedField;
import net.amygdalum.testrecorder.values.SerializedInput;
import net.amygdalum.testrecorder.values.SerializedOutput;
import net.amygdalum.testrecorder.visitors.Computation;
import net.amygdalum.testrecorder.visitors.LocalVariableNameGenerator;
import net.amygdalum.testrecorder.visitors.ObjectToMatcherCode;
import net.amygdalum.testrecorder.visitors.ObjectToSetupCode;
import net.amygdalum.testrecorder.visitors.SerializedValueVisitorFactory;
import net.amygdalum.testrecorder.visitors.TypeManager;

public class TestGenerator implements SnapshotConsumer {

	private static final Set<Class<?>> IMMUTABLE_TYPES = new HashSet<>(Arrays.asList(
		Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Float.class, Long.class, Double.class, String.class));

	private static final String RECORDED_TEST = "RecordedTest";

	private static final String TEST_FILE = "package <package>;\n\n"
		+ "<imports: {pkg | import <pkg>;\n}>"
		+ "\n\n\n"
		+ "<runner>"
		+ "public class <className> {\n"
		+ "\n"
		+ "  <fields; separator=\"\\n\">\n"
		+ "\n"
		+ "  <before>\n"
		+ "\n"
		+ "  <methods; separator=\"\\n\">"
		+ "\n}";

	private static final String RUNNER = "@RunWith(<runner>.class)\n";

	private static final String RECORDED_INPUT = "@RecordInput({<classes : {class | \"<class>\"};separator=\", \">})\n";
	private static final String RECORDED_OUTPUT = "@RecordOutput({<classes : {class | \"<class>\"};separator=\", \">})\n";

	private static final String BEFORE_TEMPLATE = "@Before\n"
		+ "public void before() throws Exception {\n"
		+ "  <statements;separator=\"\\n\">\n"
		+ "}\n";

	private static final String TEST_TEMPLATE = "@Test\n"
		+ "public void test<testName>() throws Exception {\n"
		+ "  <statements;separator=\"\\n\">\n"
		+ "}\n";

	private static final String BEGIN_ARRANGE = "\n//Arrange";
	private static final String BEGIN_ACT = "\n//Act";
	private static final String BEGIN_ASSERT = "\n//Assert";

	private TypeManager types;
	private SerializedValueVisitorFactory setup;
	private SerializedValueVisitorFactory matcher;
	private Map<Class<?>, Set<String>> tests;
	private Set<String> fields;
	private Set<String> inputClasses;
	private Set<String> outputClasses;
	private Class<? extends Runnable> initializer;

	public TestGenerator(Class<? extends Runnable> initializer) {
		this.types = initTypes();
		this.setup = new ObjectToSetupCode.Factory();
		this.matcher = new ObjectToMatcherCode.Factory();

		this.initializer = initializer;

		this.tests = synchronizedMap(new LinkedHashMap<>());
		this.fields = new LinkedHashSet<>();
		this.inputClasses = new LinkedHashSet<>();
		this.outputClasses = new LinkedHashSet<>();
	}

	private TypeManager initTypes() {
		TypeManager types = new TypeManager();
		types.registerTypes(Test.class);
		return types;
	}

	public String generateBefore(List<String> statements) {
		ST test = new ST(BEFORE_TEMPLATE);
		test.add("statements", statements);
		return test.render();
	}

	public void setSetup(SerializedValueVisitorFactory setup) {
		this.setup = setup;
	}

	public void setMatcher(SerializedValueVisitorFactory matcher) {
		this.matcher = matcher;
	}

	@Override
	public void accept(ContextSnapshot snapshot) {
		Set<String> localtests = tests.computeIfAbsent(getBase(snapshot.getThisType()), key -> new LinkedHashSet<>());

		MethodGenerator methodGenerator = new MethodGenerator(snapshot, localtests.size())
			.generateArrange()
			.generateAct()
			.generateAssert();

		localtests.add(methodGenerator.generateTest());
	}

	public void writeResults(Path dir) {
		for (Class<?> clazz : tests.keySet()) {

			String rendered = renderTest(clazz);

			try {
				Path testfile = locateTestFile(dir, clazz);
				try (Writer writer = Files.newBufferedWriter(testfile, CREATE, WRITE, TRUNCATE_EXISTING)) {
					writer.write(rendered);
				}
			} catch (IOException e) {
				System.out.println(rendered);
			}
		}
	}

	public void clearResults() {
		this.types = initTypes();
		tests.clear();
		this.fields = new LinkedHashSet<>();
		this.inputClasses = new LinkedHashSet<>();
		this.outputClasses = new LinkedHashSet<>();
	}

	private Path locateTestFile(Path dir, Class<?> clazz) throws IOException {
		String pkg = clazz.getPackage().getName();
		String className = computeClassName(clazz);
		Path testpackage = dir.resolve(pkg.replace('.', '/'));

		Files.createDirectories(testpackage);

		return testpackage.resolve(className + ".java");
	}

	public Set<String> testsFor(Class<?> clazz) {
		return tests.getOrDefault(clazz, emptySet());
	}

	public String renderTest(Class<?> clazz) {
		Set<String> localtests = testsFor(clazz);

		ST file = new ST(TEST_FILE);
		file.add("package", clazz.getPackage().getName());
		file.add("runner", computeRunner());
		file.add("className", computeClassName(clazz));
		file.add("fields", fields);
		file.add("before", computeBefore());
		file.add("methods", localtests);
		file.add("imports", types.getImports());

		return file.render();
	}

	private String computeRunner() {
		if (outputClasses.isEmpty() && inputClasses.isEmpty()) {
			return null;
		}
		if (initializer != null) {
			if (!outputClasses.isEmpty()) {
				outputClasses.add(initializer.getTypeName());
			}
			if (!inputClasses.isEmpty()) {
				inputClasses.add(initializer.getTypeName());
			}
		}

		ST runner = new ST(RUNNER);
		runner.add("runner", IORecorder.class.getSimpleName());

		ST recordedInput = new ST(RECORDED_INPUT);
		recordedInput.add("classes", inputClasses);

		ST recordedOutput = new ST(RECORDED_OUTPUT);
		recordedOutput.add("classes", outputClasses);

		return runner.render()
			+ (inputClasses.isEmpty() ? "" : recordedInput.render())
			+ (outputClasses.isEmpty() ? "" : recordedOutput.render());
	}

	private String computeBefore() {
		types.registerType(Before.class);
		if (initializer == null) {
			return "";
		}
		types.registerType(initializer);
		String initObject = newObject(types.getSimpleName(initializer));
		String initStmt = callMethodStatement(initObject, "run");
		return generateBefore(asList(initStmt));
	}

	public String computeClassName(Class<?> clazz) {
		return clazz.getSimpleName() + RECORDED_TEST;
	}

	public static TestGenerator fromRecorded(Object object) {
		try {
			Class<? extends Object> clazz = object.getClass();
			Field field = clazz.getDeclaredField(SNAPSHOT_GENERATOR_FIELD_NAME);
			field.setAccessible(true);
			SnapshotGenerator generator = (SnapshotGenerator) field.get(object);
			return (TestGenerator) generator.getMethodConsumer();
		} catch (RuntimeException | ReflectiveOperationException e) {
			return null;
		}
	}

	private class MethodGenerator {

		private LocalVariableNameGenerator locals;

		private ContextSnapshot snapshot;
		private int no;

		private List<String> statements;

		private String base;
		private List<String> args;
		private String result;

		public MethodGenerator(ContextSnapshot snapshot, int no) {
			this.snapshot = snapshot;
			this.no = no;
			this.locals = new LocalVariableNameGenerator();
			this.statements = new ArrayList<>();
		}

		public MethodGenerator generateArrange() {
			statements.add(BEGIN_ARRANGE);

			List<SerializedOutput> serializedOutput = snapshot.getExpectOutput();
			if (serializedOutput != null && !serializedOutput.isEmpty()) {
				types.registerTypes(RunWith.class, RecordOutput.class, IORecorder.class, ExpectedOutput.class);
				fields.add(fieldDeclaration("public", ExpectedOutput.class.getSimpleName(), "expectedOutput"));

				List<String> methods = new ArrayList<>();
				for (SerializedOutput out : serializedOutput) {
					types.registerImport(out.getDeclaringClass());
					outputClasses.add(out.getDeclaringClass().getTypeName());

					List<Computation> args = Stream.of(out.getValues())
						.map(arg -> arg.accept(matcher.create(locals, types)))
						.collect(toList());

					statements.addAll(args.stream()
						.flatMap(arg -> arg.getStatements().stream())
						.collect(toList()));

					List<String> arguments = Stream.concat(
						asList(classOf(out.getDeclaringClass().getSimpleName()), stringOf(out.getName())).stream(),
						args.stream()
							.map(arg -> arg.getValue()))
						.collect(toList());

					methods.add(callLocalMethod("expect", arguments));
				}
				String outputExpectation = callMethodChainStatement("expectedOutput", methods);
				statements.add(outputExpectation);
			}
			List<SerializedInput> serializedInput = snapshot.getSetupInput();
			if (serializedInput != null && !serializedInput.isEmpty()) {
				types.registerTypes(RunWith.class, RecordInput.class, IORecorder.class, SetupInput.class);
				fields.add(fieldDeclaration("public", SetupInput.class.getSimpleName(), "setupInput"));

				List<String> methods = new ArrayList<>();
				for (SerializedInput in : serializedInput) {
					types.registerImport(in.getDeclaringClass());
					inputClasses.add(in.getDeclaringClass().getTypeName());

					Computation result = null;
					if (in.getResult() != null) {
						result = in.getResult().accept(setup.create(locals, types));
						statements.addAll(result.getStatements());
					}

					List<Computation> args = Stream.of(in.getValues())
						.map(arg -> arg.accept(setup.create(locals, types)))
						.collect(toList());

					statements.addAll(args.stream()
						.flatMap(arg -> arg.getStatements().stream())
						.collect(toList()));

					List<String> arguments = new ArrayList<>();
					arguments.add(classOf(in.getDeclaringClass().getSimpleName()));
					arguments.add(stringOf(in.getName()));
					if (result != null) {
						arguments.add(result.getValue());
					} else {
						arguments.add("null");
					}
					arguments.addAll(args.stream()
						.map(arg -> arg.getValue())
						.collect(toList()));

					methods.add(callLocalMethod("provide", arguments));
				}
				String inputSetup = callMethodChainStatement("setupInput", methods);
				statements.add(inputSetup);
			}

			SerializedValueVisitor<Computation> setupCode = setup.create(locals, types);
			Computation setupThis = snapshot.getSetupThis().accept(setupCode);

			statements.addAll(setupThis.getStatements());

			List<Computation> setupArgs = Stream.of(snapshot.getSetupArgs())
				.map(arg -> arg.accept(setupCode))
				.collect(toList());

			statements.addAll(setupArgs.stream()
				.flatMap(arg -> arg.getStatements().stream())
				.collect(toList()));

			List<Computation> setupGlobals = Stream.of(snapshot.getSetupGlobals())
				.map(global -> assignGlobal(global.getDeclaringClass(), global.getName(), global.getValue().accept(setupCode)))
				.collect(toList());

			statements.addAll(setupGlobals.stream()
				.flatMap(arg -> arg.getStatements().stream())
				.collect(toList()));

			this.base = setupThis.isStored()
				? setupThis.getValue()
				: assign(snapshot.getSetupThis().getType(), setupThis.getValue());
			this.args = IntStream.range(0, setupArgs.size())
				.mapToObj(i -> setupArgs.get(i).isStored()
					? setupArgs.get(i).getValue()
					: assign(snapshot.getSetupArgs()[i].getType(), setupArgs.get(i).getValue()))
				.collect(toList());
			return this;
		}

		private Computation assignGlobal(Class<?> clazz, String name, Computation global) {
			List<String> statements = new ArrayList<>(global.getStatements());
			String base = types.getSimpleName(clazz);
			statements.add(assignFieldStatement(base, name, global.getValue()));
			String value = fieldAccess(base, name);
			return new Computation(value, global.getType(), true, statements);
		}

		public MethodGenerator generateAct() {
			statements.add(BEGIN_ACT);

			Type resultType = snapshot.getResultType();
			String methodName = snapshot.getMethodName();
			SerializedValue exception = snapshot.getExpectException();

			String statement = callMethod(base, methodName, args);
			if (resultType != void.class) {
				result = assign(resultType, statement, true);
				if (exception != null) {
					result = capture(result, exception.getValueType());
				}
			} else {
				if (exception != null) {
					result = capture(statement, exception.getValueType());
				} else {
					execute(statement);
				}
			}

			return this;
		}

		public MethodGenerator generateAssert() {
			types.staticImport(Assert.class, "assertThat");
			statements.add(BEGIN_ASSERT);

			SerializedValue exception = snapshot.getExpectException();
			if (exception == null) {
				List<String> expectResult = Optional.ofNullable(snapshot.getExpectResult())
					.map(o -> o.accept(matcher.create(locals, types)))
					.map(o -> createAssertion(o, result))
					.orElse(emptyList());

				statements.addAll(expectResult);
			} else {
				List<String> expectResult = Optional.ofNullable(exception)
					.map(o -> o.accept(matcher.create(locals, types)))
					.map(o -> createAssertion(o, result))
					.orElse(emptyList());

				statements.addAll(expectResult);
			}

			List<String> expectThis = Optional.of(snapshot.getExpectThis())
				.filter(o -> !o.equals(snapshot.getSetupThis()))
				.map(o -> o.accept(matcher.create(locals, types)))
				.map(o -> createAssertion(o, base))
				.orElse(emptyList());

			statements.addAll(expectThis);

			Type[] argumentTypes = snapshot.getArgumentTypes();
			SerializedValue[] serializedArgs = snapshot.getExpectArgs();
			List<String> expectArgs = IntStream.range(0, argumentTypes.length)
				.filter(i -> !isImmutable(argumentTypes[i]))
				.filter(i -> !serializedArgs[i].equals(snapshot.getSetupArgs()[i]))
				.mapToObj(i -> createAssertion(serializedArgs[i].accept(matcher.create(locals, types)), args.get(i)))
				.flatMap(statements -> statements.stream())
				.collect(toList());

			statements.addAll(expectArgs);

			SerializedField[] serializedGlobals = snapshot.getExpectGlobals();
			List<String> expectGlobals = IntStream.range(0, serializedGlobals.length)
				.filter(i -> !isImmutable(serializedGlobals[i].getType()))
				.filter(i -> !serializedGlobals[i].equals(snapshot.getSetupGlobals()[i]))
				.mapToObj(i -> createAssertion(serializedGlobals[i].getValue().accept(matcher.create(locals, types)),
					fieldAccess(types.getSimpleName(serializedGlobals[i].getDeclaringClass()), serializedGlobals[i].getName())))
				.flatMap(statements -> statements.stream())
				.collect(toList());

			statements.addAll(expectGlobals);

			List<SerializedOutput> serializedOutput = snapshot.getExpectOutput();
			if (serializedOutput != null && !serializedOutput.isEmpty()) {
				statements.add(callMethodStatement("expectedOutput", "verify"));
			}

			return this;
		}

		private List<String> createAssertion(Computation matcher, String exp) {

			List<String> statements = new ArrayList<>();

			statements.addAll(matcher.getStatements());

			statements.add(callLocalMethodStatement("assertThat", exp, matcher.getValue()));

			return statements;
		}

		public String assign(Type type, String value) {
			return assign(type, value, false);
		}

		public String assign(Type type, String value, boolean force) {
			if (isImmutable(type) && !force) {
				return value;
			} else {
				String name = locals.fetchName(type);

				statements.add(assignLocalVariableStatement(types.getSimpleName(type), name, value));

				return name;
			}
		}

		public void execute(String value) {
			statements.add(expressionStatement(value));
		}

		public String capture(String statement, Type type) {
			types.staticImport(Exceptions.class, "catchException");
			String name = locals.fetchName(type);

			String exceptionType = types.getRawTypeName(type);
			String capture = captureException(statement, exceptionType);

			statements.add(assignLocalVariableStatement(types.getSimpleName(type), name, capture));

			return name;
		}

		private boolean isImmutable(Type type) {
			return isPrimitive(type)
				|| IMMUTABLE_TYPES.contains(type);
		}

		public String generateTest() {
			ST test = new ST(TEST_TEMPLATE);
			test.add("testName", testName());
			test.add("statements", statements);
			return test.render();
		}

		private String testName() {
			String testName = snapshot.getMethodName();

			return toUpperCase(testName.charAt(0)) + testName.substring(1) + no;
		}

	}
}
