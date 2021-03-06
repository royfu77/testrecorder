package net.amygdalum.testrecorder.deserializers.matcher;

import static java.util.Collections.emptyList;
import static net.amygdalum.testrecorder.deserializers.Templates.equalToMatcher;
import static net.amygdalum.testrecorder.types.Computation.expression;
import static net.amygdalum.testrecorder.util.Literals.asLiteral;
import static net.amygdalum.testrecorder.util.Types.parameterized;

import org.hamcrest.Matcher;
import org.hamcrest.CoreMatchers;

import net.amygdalum.testrecorder.deserializers.Deserializer;
import net.amygdalum.testrecorder.types.Computation;
import net.amygdalum.testrecorder.types.DeserializerContext;
import net.amygdalum.testrecorder.types.TypeManager;
import net.amygdalum.testrecorder.values.SerializedLiteral;

public class DefaultLiteralAdaptor extends DefaultMatcherGenerator<SerializedLiteral> implements MatcherGenerator<SerializedLiteral> {

	@Override
	public Class<SerializedLiteral> getAdaptedClass() {
		return SerializedLiteral.class;
	}

	@Override
	public Computation tryDeserialize(SerializedLiteral value, Deserializer generator) {
		DeserializerContext context = generator.getContext();
		TypeManager types = context.getTypes();
		types.staticImport(CoreMatchers.class, "equalTo");

		String valueExpression = asLiteral(value.getValue());

		String equalToMatcher = equalToMatcher(valueExpression);
		return expression(equalToMatcher, parameterized(Matcher.class, null, value.getType()), emptyList());
	}

}
