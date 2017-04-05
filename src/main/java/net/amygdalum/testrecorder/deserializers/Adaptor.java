package net.amygdalum.testrecorder.deserializers;

import java.lang.reflect.Type;

import net.amygdalum.testrecorder.DeserializationException;
import net.amygdalum.testrecorder.SerializedValue;

public interface Adaptor<T extends SerializedValue, G> {

	Class<? extends Adaptor<T,G>> parent();
	
	Class<? extends SerializedValue> getAdaptedClass();
	
	boolean matches(Type type);
	
    default Computation tryDeserialize(T value, G generator) throws DeserializationException {
        return tryDeserialize(value, generator, DeserializerContext.NULL);
    }
    
	Computation tryDeserialize(T value, G generator, DeserializerContext context) throws DeserializationException;

}
