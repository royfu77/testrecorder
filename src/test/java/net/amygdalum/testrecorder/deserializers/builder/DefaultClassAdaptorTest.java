package net.amygdalum.testrecorder.deserializers.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.amygdalum.testrecorder.deserializers.Computation;
import net.amygdalum.testrecorder.deserializers.DefaultDeserializerContext;
import net.amygdalum.testrecorder.types.DeserializerContext;
import net.amygdalum.testrecorder.values.SerializedImmutable;

public class DefaultClassAdaptorTest {

	private DefaultClassAdaptor adaptor;
	private DeserializerContext context;

    @BeforeEach
    public void before() throws Exception {
        adaptor = new DefaultClassAdaptor();
        context = new DefaultDeserializerContext();
    }

    @Test
    public void testParentNull() throws Exception {
        assertThat(adaptor.parent()).isNull();
    }

    @Test
    public void testMatchesOnlyBigInteger() throws Exception {
        assertThat(adaptor.matches(Class.class)).isTrue();
        assertThat(adaptor.matches(Object.class)).isFalse();
    }

    @Test
    public void testTryDeserialize() throws Exception {
        SerializedImmutable<Class<?>> value = new SerializedImmutable<>(Class.class);
        value.setValue(BigInteger.class);
        SetupGenerators generator = new SetupGenerators();

        Computation result = adaptor.tryDeserialize(value, generator, context);

        assertThat(result.getStatements()).isEmpty();
        assertThat(result.getValue()).isEqualTo("java.math.BigInteger.class");
    }

}
