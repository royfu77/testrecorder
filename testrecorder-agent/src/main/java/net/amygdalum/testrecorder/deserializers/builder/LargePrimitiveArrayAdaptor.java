package net.amygdalum.testrecorder.deserializers.builder;

import static net.amygdalum.testrecorder.deserializers.Templates.callMethod;
import static net.amygdalum.testrecorder.deserializers.Templates.newObject;
import static net.amygdalum.testrecorder.types.Computation.expression;
import static net.amygdalum.testrecorder.util.Literals.asLiteral;
import static net.amygdalum.testrecorder.util.Types.baseType;
import static net.amygdalum.testrecorder.util.Types.isLiteral;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Optional;

import net.amygdalum.testrecorder.deserializers.Deserializer;
import net.amygdalum.testrecorder.hints.LoadFromFile;
import net.amygdalum.testrecorder.types.Computation;
import net.amygdalum.testrecorder.types.DeserializationException;
import net.amygdalum.testrecorder.types.DeserializerContext;
import net.amygdalum.testrecorder.types.SerializedValue;
import net.amygdalum.testrecorder.types.TypeManager;
import net.amygdalum.testrecorder.util.FileSerializer;
import net.amygdalum.testrecorder.values.SerializedArray;
import net.amygdalum.testrecorder.values.SerializedLiteral;

public class LargePrimitiveArrayAdaptor implements SetupGenerator<SerializedArray> {

	@Override
	public Class<SerializedArray> getAdaptedClass() {
		return SerializedArray.class;
	}

	@Override
	public Class<? extends SetupGenerator<SerializedArray>> parent() {
		return DefaultArrayAdaptor.class;
	}

	@Override
	public boolean matches(Type type) {
		return true;
	}

	@Override
	public Computation tryDeserialize(SerializedArray value, Deserializer generator) {
		DeserializerContext context = generator.getContext();
		TypeManager types = context.getTypes();
		Class<?> componentType = baseType(value.getComponentType());
		while (componentType.isArray()) {
			componentType = componentType.getComponentType();
		}
		if (isLiteral(componentType)) {
			Optional<LoadFromFile> hint = context.getHint(value, LoadFromFile.class);
			if (hint.isPresent()) {
				try {
					LoadFromFile loadFromFile = hint.get();
					types.registerType(FileSerializer.class);
					Object object = unwrap(value);
					String fileName = new FileSerializer(loadFromFile.writeTo()).store(object);
					String base = newObject(types.getConstructorTypeName(FileSerializer.class), asLiteral(loadFromFile.readFrom()));
					String result = callMethod(base, "load", asLiteral(fileName), types.getRawClass(value.getType()));
					return expression(result, types.mostSpecialOf(value.getUsedTypes()).orElse(Object.class));
				} catch (IOException e) {
					throw new DeserializationException("failed deserializing: " + value, e);
				}
			}
		}
		throw new DeserializationException("failed deserializing: " + value);
	}

	private Object unwrap(SerializedArray value) {
		SerializedValue[] serializedArray = value.getArray();
		Class<?> componentType = baseType(value.getComponentType());
		Object array = Array.newInstance(componentType, serializedArray.length);
		for (int i = 0; i < serializedArray.length; i++) {
			Array.set(array, i, unwrap(serializedArray[i]));
		}
		return array;
	}

	private Object unwrap(SerializedValue value) {
		if (value instanceof SerializedLiteral) {
			return ((SerializedLiteral) value).getValue();
		} else if (value instanceof SerializedArray) {
			return unwrap((SerializedArray) value);
		} else {
			throw new DeserializationException("failed deserializing: " + value);
		}
	}

}
