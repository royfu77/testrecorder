package net.amygdalum.testrecorder.deserializers;

import static net.amygdalum.testrecorder.deserializers.Computation.expression;

public class TestComputationValueVisitor extends MappedDeserializer<Computation, String> {

	public TestComputationValueVisitor() {
		super(new ValuePrinter(), s -> expression("(" + s + ")", null));
	}
}
