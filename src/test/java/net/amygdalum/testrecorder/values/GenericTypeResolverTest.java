package net.amygdalum.testrecorder.values;

import static com.almondtools.conmatch.conventions.UtilityClassMatcher.isUtilityClass;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import net.amygdalum.testrecorder.values.GenericTypeResolver;

public class GenericTypeResolverTest {

	@Test
	public void testGenericTypeResolver() throws Exception {
		assertThat(GenericTypeResolver.class, isUtilityClass());
	}

}