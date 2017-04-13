package net.amygdalum.testrecorder.util;

import java.util.Objects;

import net.amygdalum.testrecorder.deserializers.Templates;

public abstract class ValueFactory {

    public static final ValueFactory NONE =  new ValueFactory() {
        public Object newValue(java.lang.Class<?> clazz) {
            return null;
        };
    };

    public String getDescription(Class<?> clazz) {
        try {
            Object value = newValue(clazz);
            if (Types.isLiteral(clazz)) {
                return Templates.asLiteral(value);
            } else {
                return Objects.toString(value);
            }
        } catch (Exception e) {
            return "<undescribable>";
        }
    }

    public abstract Object newValue(Class<?> clazz);

}
