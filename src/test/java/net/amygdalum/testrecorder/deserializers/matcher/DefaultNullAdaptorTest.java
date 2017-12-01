package net.amygdalum.testrecorder.deserializers.matcher;

import static net.amygdalum.testrecorder.deserializers.DefaultDeserializerContext.NULL;
import static net.amygdalum.testrecorder.util.testobjects.Hidden.classOfCompletelyHidden;
import static net.amygdalum.testrecorder.util.testobjects.Hidden.classOfPartiallyHidden;
import static net.amygdalum.testrecorder.values.SerializedNull.nullInstance;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import net.amygdalum.testrecorder.deserializers.Computation;
import net.amygdalum.testrecorder.util.testobjects.Hidden;
import net.amygdalum.testrecorder.values.SerializedNull;

public class DefaultNullAdaptorTest {

	private DefaultNullAdaptor adaptor;

    @Before
    public void before() throws Exception {
        adaptor = new DefaultNullAdaptor();
    }

    @Test
    public void testParentNull() throws Exception {
        assertThat(adaptor.parent(), nullValue());
    }

    @Test
    public void testMatchesAny() throws Exception {
        assertThat(adaptor.matches(Object.class), is(true));
        assertThat(adaptor.matches(new Object() {
        }.getClass()), is(true));
    }

    @Test
    public void testTryDeserialize() throws Exception {
        SerializedNull value = nullInstance(String.class);
        MatcherGenerators generator = new MatcherGenerators(getClass());

        Computation result = adaptor.tryDeserialize(value, generator, NULL);

        assertThat(result.getStatements(), empty());
        assertThat(result.getValue(), equalTo("nullValue(String.class)"));
    }

    @Test
    public void testTryDeserializeOnHidden() throws Exception {
        SerializedNull value = nullInstance(classOfPartiallyHidden());
        value.setResultType(Hidden.VisibleInterface.class);

        MatcherGenerators generator = new MatcherGenerators(getClass());

        Computation result = adaptor.tryDeserialize(value, generator, NULL);

        assertThat(result.getStatements(), empty());
        assertThat(result.getValue(), equalTo("nullValue(net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface.class)"));
    }

    @Test
    public void testTryDeserializeOnReallyHidden() throws Exception {
        SerializedNull value = nullInstance(classOfCompletelyHidden());
        value.setResultType(classOfCompletelyHidden());

        MatcherGenerators generator = new MatcherGenerators(getClass());

        Computation result = adaptor.tryDeserialize(value, generator, NULL);

        assertThat(result.getStatements(), empty());
        assertThat(result.getValue(), equalTo("nullValue()"));
    }

}
