package net.amygdalum.testrecorder.deserializers.matcher;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static net.amygdalum.testrecorder.deserializers.Templates.containsInAnyOrderMatcher;
import static net.amygdalum.testrecorder.deserializers.Templates.emptyMatcher;
import static net.amygdalum.testrecorder.deserializers.TypeManager.parameterized;
import static net.amygdalum.testrecorder.deserializers.TypeManager.wildcard;

import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import net.amygdalum.testrecorder.deserializers.Adaptor;
import net.amygdalum.testrecorder.deserializers.Computation;
import net.amygdalum.testrecorder.deserializers.DefaultAdaptor;
import net.amygdalum.testrecorder.deserializers.TypeManager;
import net.amygdalum.testrecorder.values.SerializedSet;

public class DefaultSetAdaptor extends DefaultAdaptor<SerializedSet, ObjectToMatcherCode> implements Adaptor<SerializedSet, ObjectToMatcherCode> {

	@Override
	public Computation tryDeserialize(SerializedSet value, ObjectToMatcherCode generator) {
		TypeManager types = generator.getTypes();
		if (value.isEmpty()) {
			types.staticImport(Matchers.class, "empty");

			String emptyMatcher = emptyMatcher();
			return new Computation(emptyMatcher, parameterized(Matcher.class, null, wildcard()), emptyList());
		} else {
			types.staticImport(Matchers.class, "containsInAnyOrder");

			List<Computation> elements = value.stream()
				.map(element -> generator.simpleValue(element))
				.collect(toList());

			List<String> elementComputations = elements.stream()
				.flatMap(element -> element.getStatements().stream())
				.collect(toList());

			String[] elementValues = elements.stream()
				.map(element -> element.getValue())
				.toArray(String[]::new);

			String containsInAnyOrderMatcher = containsInAnyOrderMatcher(elementValues);
			return new Computation(containsInAnyOrderMatcher, parameterized(Matcher.class, null, wildcard()), elementComputations);
		}
	}

}
