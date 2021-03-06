package net.amygdalum.testrecorder.deserializers;

import static net.amygdalum.testrecorder.util.Types.baseType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.amygdalum.testrecorder.runtime.GenericObject;
import net.amygdalum.testrecorder.runtime.GenericObjectException;
import net.amygdalum.testrecorder.types.DeserializationException;
import net.amygdalum.testrecorder.types.DeserializerContext;
import net.amygdalum.testrecorder.types.RoleVisitor;
import net.amygdalum.testrecorder.types.SerializedArgument;
import net.amygdalum.testrecorder.types.SerializedField;
import net.amygdalum.testrecorder.types.SerializedImmutableType;
import net.amygdalum.testrecorder.types.SerializedReferenceType;
import net.amygdalum.testrecorder.types.SerializedResult;
import net.amygdalum.testrecorder.types.SerializedValue;
import net.amygdalum.testrecorder.types.SerializedValueType;
import net.amygdalum.testrecorder.values.SerializedArray;
import net.amygdalum.testrecorder.values.SerializedEnum;
import net.amygdalum.testrecorder.values.SerializedImmutable;
import net.amygdalum.testrecorder.values.SerializedList;
import net.amygdalum.testrecorder.values.SerializedLiteral;
import net.amygdalum.testrecorder.values.SerializedMap;
import net.amygdalum.testrecorder.values.SerializedNull;
import net.amygdalum.testrecorder.values.SerializedObject;
import net.amygdalum.testrecorder.values.SerializedSet;

public class SimpleDeserializer implements RoleVisitor<Object> {

	private DeserializerContext context;
	private Map<SerializedValue, Object> deserialized;

	public SimpleDeserializer(DeserializerContext context) {
		this.context = context;
		this.deserialized = new IdentityHashMap<>();
	}
	
	public DeserializerContext getContext() {
		return context;
	}

	@SuppressWarnings("unchecked")
	private <T> T fetch(SerializedValue key, Supplier<T> supplier, Consumer<T> init) {
		T value = (T) deserialized.get(key);
		if (value == null) {
			value = supplier.get();
			deserialized.put(key, value);
			init.accept(value);
		}
		return value;
	}

	@Override
	public Object visitField(SerializedField field) {
		throw new DeserializationException("failed deserializing: " + field);
	}

	@Override
	public Object visitArgument(SerializedArgument argument) {
		throw new DeserializationException("failed deserializing: " + argument);
	}

	@Override
	public Object visitResult(SerializedResult result) {
		throw new DeserializationException("failed deserializing: " + result);
	}

	@Override
	public Object visitReferenceType(SerializedReferenceType rt) {
		if (rt instanceof SerializedObject) {
			SerializedObject value = (SerializedObject) rt;
			try {
				Object object = fetch(value, () -> GenericObject.newInstance(baseType(value.getType())), base -> {
					for (SerializedField field : value.getFields()) {
						GenericObject.setField(base, field.getName(), field.getValue().accept(this));
					}
				});
				return object;
			} catch (GenericObjectException e) {
				throw new DeserializationException("failed deserializing: " + value, e);
			}
		} else if (rt instanceof SerializedList) {
			SerializedList value = (SerializedList) rt;
			List<Object> list = fetch(value, ArrayList::new, base -> {
				for (SerializedValue element : value) {
					base.add(element.accept(this));
				}
			});
			return list;
		} else if (rt instanceof SerializedMap) {
			SerializedMap value = (SerializedMap) rt;
			Map<Object, Object> map = fetch(value, LinkedHashMap::new, base -> {
				for (Map.Entry<SerializedValue, SerializedValue> entry : value.entrySet()) {
					Object k = entry.getKey().accept(this);
					Object v = entry.getValue().accept(this);
					base.put(k, v);
				}
			});
			return map;
		} else if (rt instanceof SerializedSet) {
			SerializedSet value = (SerializedSet) rt;
			Set<Object> set = fetch(value, LinkedHashSet::new, base -> {
				for (SerializedValue element : value) {
					base.add(element.accept(this));
				}
			});
			return set;
		} else if (rt instanceof SerializedArray) {
			SerializedArray value = (SerializedArray) rt;
			Class<?> componentType = value.getRawType();
			SerializedValue[] rawArray = value.getArray();
			Object array = fetch(value, () -> Array.newInstance(componentType, rawArray.length), base -> {
				for (int i = 0; i < rawArray.length; i++) {
					Array.set(base, i, rawArray[i].accept(this));
				}
			});
			return array;
		} else if (rt instanceof SerializedNull) {
			return null;
		} else {
			throw new DeserializationException("failed deserializing: " + rt);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object visitImmutableType(SerializedImmutableType rt) {
		if (rt instanceof SerializedImmutable<?>) {
			SerializedImmutable<?> value = (SerializedImmutable<?>) rt;
			return fetch(value, () -> value.getValue(), noInit());
		} else if (rt instanceof SerializedEnum) {
			SerializedEnum value = (SerializedEnum) rt;
			return Enum.valueOf((Class<? extends Enum>) baseType(value.getType()), value.getName());
		} else {
			return null;
		}
	}

	@Override
	public Object visitValueType(SerializedValueType value) {
		return fetch(value, () -> ((SerializedLiteral) value).getValue(), noInit());
	}

	private <T> Consumer<T> noInit() {
		return base -> {
		};
	}

}
