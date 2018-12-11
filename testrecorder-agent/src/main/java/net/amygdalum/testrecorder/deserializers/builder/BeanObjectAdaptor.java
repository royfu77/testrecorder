package net.amygdalum.testrecorder.deserializers.builder;

import java.lang.reflect.Type;

import net.amygdalum.testrecorder.deserializers.Deserializer;
import net.amygdalum.testrecorder.types.Computation;
import net.amygdalum.testrecorder.types.DeserializationException;
import net.amygdalum.testrecorder.types.DeserializerContext;
import net.amygdalum.testrecorder.types.TypeManager;
import net.amygdalum.testrecorder.values.SerializedObject;

public class BeanObjectAdaptor implements SetupGenerator<SerializedObject> {

	@Override
	public Class<SerializedObject> getAdaptedClass() {
		return SerializedObject.class;
	}

	@Override
	public Class<? extends SetupGenerator<SerializedObject>> parent() {
		return DefaultObjectAdaptor.class;
	}

	@Override
	public boolean matches(Type type) {
		return true;
	}

	@Override
	public Computation tryDeserialize(SerializedObject value, Deserializer generator) throws DeserializationException {
		DeserializerContext context = generator.getContext();
		TypeManager types = context.getTypes();

		Type type = types.isHidden(value.getType())
			? types.mostSpecialOf(value.getUsedTypes()).orElse(Object.class)
			: value.getType();

		return context.forVariable(value, type, local -> {
			try {
				Computation bean = new Construction(context, local, value).computeBest(types, generator);
				return bean;
			} catch (ReflectiveOperationException | RuntimeException e) {
				throw new DeserializationException("failed deserializing as bean: " + value, e);
			}
		});
	}

}
