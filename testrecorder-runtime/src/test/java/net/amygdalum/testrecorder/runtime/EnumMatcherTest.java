package net.amygdalum.testrecorder.runtime;

import static net.amygdalum.testrecorder.runtime.EnumMatcher.matchingEnum;
import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EnumMatcherTest {

	@Test
	void testDescribeTo() throws Exception {
		StringDescription description = new StringDescription();

		matchingEnum("VALUE").describeTo(description);

		assertThat(description.toString()).isEqualTo("with name \"VALUE\"");
	}

	@Nested
	class testMatchesSafely {
		@Test
		void withSuccess() throws Exception {
			boolean matches = matchingEnum("VALUE").matchesSafely((Enum<?>) MyEnum.VALUE);

			assertThat(matches).isTrue();
		}

		@Test
		void withFailure() throws Exception {
			boolean matches = matchingEnum("NOVALUE").matchesSafely((Enum<?>) MyEnum.VALUE);

			assertThat(matches).isFalse();
		}
	}
}

enum MyEnum {
	VALUE;
}
