package net.amygdalum.testrecorder.deserializers.builder;

import static net.amygdalum.testrecorder.deserializers.Computation.expression;
import static net.amygdalum.testrecorder.deserializers.Templates.asLiteral;

import net.amygdalum.testrecorder.deserializers.Computation;
import net.amygdalum.testrecorder.deserializers.DeserializerContext;
import net.amygdalum.testrecorder.values.SerializedLiteral;

public class DefaultLiteralAdaptor extends DefaultSetupGenerator<SerializedLiteral> implements SetupGenerator<SerializedLiteral> {

	@Override
	public Class<SerializedLiteral> getAdaptedClass() {
		return SerializedLiteral.class;
	}

	@Override
	public Computation tryDeserialize(SerializedLiteral value, SetupGenerators generator, DeserializerContext context) {
		Object literalValue = value.getValue();
		String literal = asLiteral(literalValue);
		return expression(literal, value.getResultType());
	}

}
