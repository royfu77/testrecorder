package net.amygdalum.testrecorder.types;

import static net.amygdalum.testrecorder.types.SerializedRole.NO_ANNOTATIONS;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class AbstractSerializedInteraction implements SerializedInteraction, Serializable {

	protected int id;
	protected Class<?> clazz;
	protected String name;
	protected Type resultType;
	protected SerializedResult result;
	protected Type[] types;
	protected SerializedArgument[] arguments;

	public AbstractSerializedInteraction(int id, Class<?> clazz, String name, Type resultType, Type[] types) {
		assert resultType instanceof Serializable;
		assert Arrays.stream(types).allMatch(type -> type instanceof Serializable);
		this.id = id;
		this.clazz = clazz;
		this.name = name;
		this.resultType = resultType;
		this.types = types;
		this.arguments = new SerializedArgument[0];
	}

	public int id() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean isStatic() {
		return id == STATIC;
	}

	@Override
	public boolean isComplete() {
		if (resultType == null || types == null) {
			return false;
		}
		if (result == null) {
			return false;
		}
		if (arguments == null || arguments.length != types.length) {
			return false;
		}
		return true;
	}

	@Override
	public boolean hasResult() {
		return resultType != null
			&& resultType != void.class
			&& result != null;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public Class<?> getDeclaringClass() {
		return clazz;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Type getResultType() {
		return resultType;
	}

	@Override
	public SerializedResult getResult() {
		return result;
	}

	@Override
	public Type[] getTypes() {
		return types;
	}

	@Override
	public SerializedArgument[] getArguments() {
		return arguments;
	}

	@Override
	public List<SerializedValue> getAllValues() {
		List<SerializedValue> allValues = new ArrayList<>();
		allValues.add(result.getValue());
		for (SerializedArgument argument : arguments) {
			allValues.add(argument.getValue());
		}
		return allValues;
	}

	@Override
	public int hashCode() {
		return clazz.hashCode() * 37
			+ name.hashCode() * 29
			+ resultType.hashCode() * 17
			+ (result == null ? 0 : result.hashCode() * 13)
			+ Arrays.hashCode(types) * 11
			+ Arrays.hashCode(arguments);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AbstractSerializedInteraction that = (AbstractSerializedInteraction) obj;
		return this.id == that.id
			&& this.clazz.equals(that.clazz)
			&& this.name.equals(that.name)
			&& this.resultType.equals(that.resultType)
			&& Objects.equals(this.result, that.result)
			&& Arrays.equals(this.types, that.types)
			&& Arrays.equals(this.arguments, that.arguments);
	}

	protected SerializedArgument[] argumentsOf(SerializedValue[] argumentValues) {
		if (argumentValues == null) {
			return new SerializedArgument[0];
		} else {
			SerializedArgument[] arguments = new SerializedArgument[argumentValues.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = new SerializedArgument(i, types[i], NO_ANNOTATIONS, argumentValues[i]);
			}
			return arguments;
		}
	}

	protected SerializedResult resultOf(SerializedValue resultValue) {
		if (resultValue == null) {
			return null;
		} else {
			return new SerializedResult(resultType, NO_ANNOTATIONS, resultValue);
		}
	}

}