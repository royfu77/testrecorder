package net.amygdalum.testrecorder;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static net.amygdalum.testrecorder.asm.ByteCode.classFrom;
import static net.amygdalum.testrecorder.util.Reflections.accessing;
import static net.amygdalum.testrecorder.util.Types.baseType;
import static net.amygdalum.testrecorder.util.Types.isLiteral;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import net.amygdalum.testrecorder.profile.AgentConfiguration;
import net.amygdalum.testrecorder.profile.Classes;
import net.amygdalum.testrecorder.profile.Fields;
import net.amygdalum.testrecorder.profile.SerializationProfile;
import net.amygdalum.testrecorder.profile.SerializationProfile.Excluded;
import net.amygdalum.testrecorder.serializers.ArraySerializer;
import net.amygdalum.testrecorder.serializers.EnumSerializer;
import net.amygdalum.testrecorder.serializers.GenericSerializer;
import net.amygdalum.testrecorder.serializers.LambdaSerializer;
import net.amygdalum.testrecorder.serializers.SerializerFacade;
import net.amygdalum.testrecorder.types.Profile;
import net.amygdalum.testrecorder.types.SerializationException;
import net.amygdalum.testrecorder.types.SerializedReferenceType;
import net.amygdalum.testrecorder.types.SerializedValue;
import net.amygdalum.testrecorder.types.Serializer;
import net.amygdalum.testrecorder.types.SerializerSession;
import net.amygdalum.testrecorder.util.Lambdas;
import net.amygdalum.testrecorder.util.Logger;
import net.amygdalum.testrecorder.values.SerializedField;
import net.amygdalum.testrecorder.values.SerializedLiteral;
import net.amygdalum.testrecorder.values.SerializedNull;

public class ConfigurableSerializerFacade implements SerializerFacade {

	private Map<Class<?>, Serializer<?>> serializers;
	private List<Classes> classExclusions;
	private List<Fields> fieldExclusions;

	public ConfigurableSerializerFacade(AgentConfiguration config) {
		serializers = setupSerializers(config, this);
		classExclusions = classExclusions(config);
		fieldExclusions = fieldExclusions(config);
	}

	private static List<Classes> classExclusions(AgentConfiguration config) {
		List<Classes> excluded = new ArrayList<>(config.loadConfiguration(SerializationProfile.class)
			.getClassExclusions());
		excluded.addAll(testrecorderClasses());
		return excluded;
	}

	private static List<Classes> testrecorderClasses() {
		return asList(
			Classes.byDescription(SnapshotManager.class),
			Classes.byDescription(ContextSnapshot.class),
			Classes.byDescription(SerializerFacade.class),
			Classes.byDescription(ConfigurableSerializerFacade.class),
			Classes.byDescription(Profile.class),
			Classes.byDescription(Logger.class),
			Classes.byPackage("net.amygdalum.testrecorder.values"));
	}

	private static List<Fields> fieldExclusions(AgentConfiguration config) {
		List<Fields> excluded = new ArrayList<>(config.loadConfiguration(SerializationProfile.class)
			.getFieldExclusions());
		return excluded;
	}

	private static Map<Class<?>, Serializer<?>> setupSerializers(AgentConfiguration config, SerializerFacade facade) {
		IdentityHashMap<Class<?>, Serializer<?>> serializers = new IdentityHashMap<>();
		for (Serializer<?> serializer : config.loadConfigurations(Serializer.class, facade)) {
			for (Class<?> clazz : serializer.getMatchingClasses()) {
				serializers.put(clazz, serializer);
			}
		}
		return serializers;
	}

	@Override
	public SerializedValue serialize(Type type, Object object, SerializerSession session) {
		if (object == null) {
			return SerializedNull.nullInstance(type);
		} else if (isLiteral(object.getClass()) && baseType(type).isPrimitive()) {
			return SerializedLiteral.literal(type, object);
		} else if (isLiteral(object.getClass())) {
			return SerializedLiteral.literal(object);
		} else if (Lambdas.isSerializableLambda(object.getClass())) {
			return createLambdaObject(type, object, session);
		} else {
			return createObject(type, object, session);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializedValue createLambdaObject(Type type, Object object, SerializerSession session) {
		Profile serialization = session.log(type);
		SerializedValue serializedObject = session.find(object);
		if (serializedObject == null) {
			SerializedLambda serializedLambda = Lambdas.serializeLambda(object);
			try {
				Class<?> functionalInterfaceType = classFrom(serializedLambda.getFunctionalInterfaceClass());
				Serializer serializer = fetchSerializer(serializedLambda.getClass());
				serializedObject = serializer.generate(functionalInterfaceType, session);
				session.resolve(object, serializedObject);
				if (serializedObject instanceof SerializedReferenceType) {
					SerializedReferenceType serializedReferenceType = (SerializedReferenceType) serializedObject;
					serializedReferenceType.useAs(type);
					serializedReferenceType.setId(identityHashCode(object));
				}
				serializer.populate(serializedObject, serializedLambda, session);
			} catch (RuntimeException e) {
				throw new SerializationException(e);
			}
		}
		serialization.stop();
		return serializedObject;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializedValue createObject(Type type, Object object, SerializerSession session) {
		Profile serialization = session.log(type);
		SerializedValue serializedObject = session.find(object);
		if (serializedObject == null) {
			Serializer serializer = fetchSerializer(object.getClass());
			serializedObject = serializer.generate(object.getClass(), session);
			session.resolve(object, serializedObject);
			if (serializedObject instanceof SerializedReferenceType) {
				SerializedReferenceType serializedReferenceType = (SerializedReferenceType) serializedObject;
				serializedReferenceType.setId(identityHashCode(object));
				serializedReferenceType.useAs(type);
			}
			serializer.populate(serializedObject, object, session);
		} else if (serializedObject instanceof SerializedReferenceType) {
			SerializedReferenceType serializedReferenceType = (SerializedReferenceType) serializedObject;
			serializedReferenceType.useAs(type);
		}
		serialization.stop();
		return serializedObject;
	}

	private Serializer<?> fetchSerializer(Class<?> clazz) {
		Serializer<?> serializer = serializers.get(clazz);
		if (serializer != null) {
			return serializer;
		}
		if (clazz.isArray()) {
			serializer = new ArraySerializer(this);
		} else if (clazz.isEnum() || (clazz.getSuperclass() != null && clazz.getSuperclass().isEnum())) {
			serializer = new EnumSerializer(this);
		} else if (SerializedLambda.class == clazz) {
			serializer = new LambdaSerializer(this);
		} else {
			serializer = new GenericSerializer(this);
		}
		return serializer;
	}

	@Override
	public SerializedValue[] serialize(Type[] clazzes, Object[] objects, SerializerSession session) {
		return IntStream.range(0, clazzes.length)
			.mapToObj(i -> serialize(clazzes[i], objects[i], session))
			.toArray(SerializedValue[]::new);
	}

	@Override
	public SerializedField serialize(Field field, Object obj, SerializerSession session) {
		try {
			return accessing(field).call(f -> createField(f, obj, session));
		} catch (ReflectiveOperationException e) {
			throw new SerializationException(e);
		}
	}

	private SerializedField createField(Field field, Object obj, SerializerSession session) throws IllegalAccessException {
		Class<?> declaringClass = field.getDeclaringClass();
		String name = field.getName();
		Type type = field.getGenericType();
		SerializedValue serializedObject = serialize(type, field.get(obj), session);
		SerializedField serializedField = new SerializedField(declaringClass, name, type, serializedObject);

		return serializedField;
	}

	@Override
	public boolean excludes(Field field) {
		if (field.isAnnotationPresent(Excluded.class)) {
			return true;
		}
		boolean excluded = fieldExclusions.stream()
			.anyMatch(exclusion -> exclusion.matches(field));
		if (!excluded) {
			Class<?> type = field.getType();
			excluded = classExclusions.stream()
				.anyMatch(exclusion -> exclusion.matches(type));
		}
		return excluded;
	}

	@Override
	public boolean excludes(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Excluded.class)) {
			return true;
		}
		return classExclusions.stream()
			.anyMatch(exclusion -> exclusion.matches(clazz));
	}

}
