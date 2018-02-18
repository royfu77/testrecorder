package net.amygdalum.testrecorder.serializers;

import static java.util.stream.Collectors.toList;
import static net.amygdalum.testrecorder.util.TypeFilters.startingWith;
import static net.amygdalum.testrecorder.util.Types.inferType;
import static net.amygdalum.testrecorder.util.Types.parameterized;
import static net.amygdalum.testrecorder.util.Types.typeArgument;
import static net.amygdalum.testrecorder.util.Types.visibleType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import net.amygdalum.testrecorder.util.Reflections;
import net.amygdalum.testrecorder.values.SerializedMap;

public class CollectionsMapSerializer extends HiddenInnerClassSerializer<SerializedMap> {

	public CollectionsMapSerializer(SerializerFacade facade) {
		super(Collections.class, facade);
	}

	@Override
	public List<Class<?>> getMatchingClasses() {
		return innerClasses().stream()
			.filter(startingWith("Unmodifiable", "Synchronized", "Checked", "Empty", "Singleton"))
			.filter(clazz -> Map.class.isAssignableFrom(clazz))
			.collect(toList());
	}

	@Override
	public SerializedMap generate(Type resultType, Type type) {
		return new SerializedMap(type).withResult(resultType);
	}

	@Override
	public void populate(SerializedMap serializedObject, Object object) {
		Type[] componentTypes = computeComponentType(serializedObject, object);

		for (Map.Entry<?, ?> element : ((Map<?, ?>) object).entrySet()) {
			Object key = element.getKey();
			Object value = element.getValue();
			Type keyType = visibleType(key,  componentTypes[0]); 
			Type valueType = visibleType(value,  componentTypes[1]);
			serializedObject.put(facade.serialize(keyType, key), facade.serialize(valueType, value));
		}
		Type newType = parameterized(Map.class, null, componentTypes);
		serializedObject.setResultType(newType);
	}

	private Type[] computeComponentType(SerializedMap serializedObject, Object object) {
		if (object.getClass().getSimpleName().contains("Checked")) {
			return new Type[] { getKeyTypeField(object), getValueTypeField(object) };
		}
		Type resultType = serializedObject.getResultType();

		Stream<Type> keyTypes = Stream.concat(
			Stream.of(typeArgument(resultType, 0).orElse(Object.class)),
			((Map<?, ?>) object).keySet().stream()
				.filter(Objects::nonNull)
				.map(element -> element.getClass()));

		Stream<Type> valueTypes = Stream.<Type> concat(
			Stream.of(typeArgument(resultType, 1).orElse(Object.class)),
			((Map<?, ?>) object).values().stream()
				.filter(Objects::nonNull)
				.map(element -> element.getClass()));
		return new Type[] { inferType(keyTypes.toArray(Type[]::new)), inferType(valueTypes.toArray(Type[]::new)) };
	}

	private Class<?> getKeyTypeField(Object object) {
		try {
			return (Class<?>) Reflections.getValue("keyType", object);
		} catch (ReflectiveOperationException e) {
			return Object.class;
		}
	}

	private Class<?> getValueTypeField(Object object) {
		try {
			return (Class<?>) Reflections.getValue("valueType", object);
		} catch (ReflectiveOperationException e) {
			return Object.class;
		}
	}

	public static class Factory implements SerializerFactory<SerializedMap> {

		@Override
		public CollectionsMapSerializer newSerializer(SerializerFacade facade) {
			return new CollectionsMapSerializer(facade);
		}

	}

}
