package net.amygdalum.testrecorder.util;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static net.amygdalum.testrecorder.util.Params.NONE;
import static net.amygdalum.testrecorder.util.Reflections.accessing;
import static net.amygdalum.testrecorder.util.Types.getDeclaredField;
import static net.amygdalum.testrecorder.util.Types.isFinal;
import static net.amygdalum.testrecorder.util.Types.isStatic;
import static net.amygdalum.testrecorder.util.Types.isUnhandledSynthetic;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.amygdalum.testrecorder.Wrapped;

public abstract class GenericObject {

    public static <T> T forward(Class<T> clazz) {
        return newInstance(clazz);
    }

    public static Wrapped forward(Wrapped wrapped) {
        return wrapped;
    }

    public static void define(Object o, GenericObject genericObject) {
        Object value = o instanceof Wrapped ? ((Wrapped) o).value() : o;
        for (Field field : genericObject.getGenericFields()) {
            try {
                accessing(field).exec(() -> setField(value, field.getName(), field.get(genericObject)));
            } catch (ReflectiveOperationException e) {
                throw new GenericObjectException(e);
            }
        }
    }

    public <T> T as(Class<T> clazz) {
        return as(newInstance(clazz));
    }

    @SuppressWarnings("unchecked")
    public static <T> T newProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
        });
    }

    public static <T> T newEnum(Class<T> clazz) {
        try {
            Field valueField = clazz.getDeclaredField("$VALUES");
            T value = accessing(valueField).call(() -> {
                Object values = valueField.get(null);
                if (values.getClass().isArray() && Array.getLength(values) > 0) {
                    return clazz.cast(Array.get(values, 0));
                } else {
                    return null;
                }
            });
            return value;
        } catch (ReflectiveOperationException e) {
            throw new GenericObjectException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "restriction" })
    public static <T> T newInstance(Class<T> clazz) {
        List<String> tries = new ArrayList<>();
        for (Constructor<T> constructor : (Constructor<T>[]) clazz.getDeclaredConstructors()) {
            try {
                return accessing(constructor).call(() -> {
                    List<String> innertries = new ArrayList<>();
                    for (Params params : bestParams(constructor.getParameterTypes())) {
                        try {
                            return constructor.newInstance(params.values());
                        } catch (ReflectiveOperationException | RuntimeException e) {
                            innertries.add("new " + clazz.getSimpleName() + params.getDescription());
                        }
                    }
                    throw new FailedInstantiationException(clazz, innertries);
                });
            } catch (ReflectiveOperationException e) {
                throw new GenericObjectException(e);
            } catch (FailedInstantiationException e) {
                tries.addAll(e.getTries());
            }
        }
        try {
            sun.reflect.ReflectionFactory rf = sun.reflect.ReflectionFactory.getReflectionFactory();
            Constructor<T> serializationConstructor = (Constructor<T>) rf.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
            return clazz.cast(serializationConstructor.newInstance());
        } catch (ReflectiveOperationException | RuntimeException | Error e) {
            throw new FailedInstantiationException(clazz, tries);
        }
    }

    private static Iterable<Params> bestParams(Class<?>[] parameterTypes) {
        if (parameterTypes.length == 0) {
            return asList(NONE);
        } else {
            return asList(
                new Params(parameterTypes, DefaultValue.INSTANCE),
                new Params(parameterTypes, NonNullValue.INSTANCE),
                new Params(parameterTypes, NonDefaultValue.INSTANCE));
        }
    }

    public <T> T as(Supplier<T> constructor) {
        return as(constructor.get());
    }

    public Wrapped as(Wrapped wrapped) {
        for (Field field : getGenericFields()) {
            try {
                accessing(field).exec(() -> wrapped.setField(field.getName(), field.get(this)));
            } catch (ReflectiveOperationException e) {
                throw new GenericObjectException(e);
            }
        }
        return wrapped;
    }

    public <T> T as(T o) {
        for (Field field : getGenericFields()) {
            try {
                accessing(field).exec(() -> setField(o, field.getName(), field.get(this)));
            } catch (ReflectiveOperationException e) {
                throw new GenericObjectException(e);
            }
        }
        return o;
    }

    public static void setField(Object o, String name, Object value) {
        if (value instanceof Wrapped) {
            value = ((Wrapped) value).value();
        }
        Field to = findField(name, o.getClass());
        setField(o, to, value);
    }

    public static void setField(Object o, Field to, Object value) {
        try {
            accessing(to).exec(() -> to.set(o, value));
        } catch (ReflectiveOperationException e) {
            throw new GenericObjectException(e);
        }
    }

    public static void copyArrayValues(Object from, Object to) {
        int fromLength = Array.getLength(from);
        int toLength = Array.getLength(to);
        if (fromLength != toLength) {
            throw new GenericObjectException(new ArrayIndexOutOfBoundsException());
        }
        for (int i = 0; i < fromLength; i++) {
            Object value = Array.get(from, i);
            Array.set(to, i, value);
        }
    }

    public static void copyField(Field field, Object from, Object to) {
        try {
            accessing(field).exec(() -> {
                Object value = field.get(from);
                field.set(to, value);
            });
        } catch (ReflectiveOperationException e) {
            throw new GenericObjectException(e);
        }
    }

    public static Field findField(String name, Class<?> clazz) {
        try {
            return getDeclaredField(clazz, name);
        } catch (NoSuchFieldException e) {
            throw new GenericObjectException(e);
        }
    }

    public List<Field> getGenericFields() {
        Field[] declaredFields = getClass().getDeclaredFields();
        return Stream.of(declaredFields)
            .filter(field -> isSerializable(field))
            .collect(toList());
    }

    private boolean isSerializable(Field field) {
        if (isUnhandledSynthetic(field)) {
            return false;
        }
        return !isStatic(field)
            && !isFinal(field);
    }

}
