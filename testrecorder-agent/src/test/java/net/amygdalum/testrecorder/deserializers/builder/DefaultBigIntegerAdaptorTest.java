package net.amygdalum.testrecorder.deserializers.builder;

import static net.amygdalum.testrecorder.TestAgentConfiguration.defaultConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.amygdalum.testrecorder.deserializers.Adaptors;
import net.amygdalum.testrecorder.deserializers.DefaultDeserializerContext;
import net.amygdalum.testrecorder.deserializers.Deserializer;
import net.amygdalum.testrecorder.profile.AgentConfiguration;
import net.amygdalum.testrecorder.types.Computation;
import net.amygdalum.testrecorder.types.DeserializerContext;
import net.amygdalum.testrecorder.values.SerializedImmutable;

public class DefaultBigIntegerAdaptorTest {

	private AgentConfiguration config;
	private DefaultBigIntegerAdaptor adaptor;
	private DeserializerContext context;

	@BeforeEach
	public void before() throws Exception {
		config = defaultConfig();
		adaptor = new DefaultBigIntegerAdaptor();
		context = new DefaultDeserializerContext();
	}

	@Test
	public void testParentNull() throws Exception {
		assertThat(adaptor.parent()).isNull();
	}

	@Test
	public void testMatchesOnlyBigInteger() throws Exception {
		assertThat(adaptor.matches(BigInteger.class)).isTrue();
		assertThat(adaptor.matches(Object.class)).isFalse();
	}

	@Test
	public void testTryDeserialize() throws Exception {
		SerializedImmutable<BigInteger> value = new SerializedImmutable<>(BigInteger.class);
		value.setValue(new BigInteger("0815"));
		Deserializer generator = generator();

		Computation result = adaptor.tryDeserialize(value, generator);

		assertThat(result.getStatements()).isEmpty();
		assertThat(result.getValue()).isEqualTo("new BigInteger(\"815\")");
	}

	private Deserializer generator() {
		return new SetupGenerators(new Adaptors().load(config.loadConfigurations(SetupGenerator.class))).newGenerator(context);
	}

}
