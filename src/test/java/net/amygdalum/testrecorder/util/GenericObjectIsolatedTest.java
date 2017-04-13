package net.amygdalum.testrecorder.util;

import static com.almondtools.conmatch.conventions.ReflectiveEqualsMatcher.reflectiveEqualTo;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.security.Permission;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.binarytweed.test.Quarantine;
import com.binarytweed.test.QuarantiningRunner;

import net.amygdalum.testrecorder.Wrapped;

@SuppressWarnings("unused")
@RunWith(InstrumentedClassLoaderRunner.class)
public class GenericObjectIsolatedTest {

    @Test
    public void testNewInstanceFailingBruteForceReflection() throws Exception {
        WithSecurityManager.with(new SecurityManager() {
                @Override
                public void checkPackageAccess(String pkg) {
                    if (pkg.equals("sun.reflect")) {
                        throw new SecurityException("security manager preventing reflection");
                    }
                }

                @Override
                public void checkPermission(Permission perm) {
                }
            }).execute(() -> {
                Throwable captured = Throwables.capture(() -> GenericObject.newInstance(NonSerializableConstructor.class));

                assertThat(captured.getMessage(), allOf(
                    containsString("NonSerializableConstructor(null)"),
                    containsString("NonSerializableConstructor(\"\")"),
                    containsString("NonSerializableConstructor(\"String\")")));
            });
    }
    
    private interface AnInterface {
    }

    private enum AnEnum {
        ENUM;
    }

    private enum EmptyEnum {
    }

    private static class Simple {
        private String str;

        public Simple() {
        }

        public Simple(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }

    private static class Complex {

        private Simple simple;

        public Complex() {
            this.simple = new Simple("otherStr");
        }

        public Simple getSimple() {
            return simple;
        }
    }

    private static class SimplePrivateConstructor {
        private String str;

        private SimplePrivateConstructor() {
        }

        public String getStr() {
            return str;
        }
    }

    private static class SimpleImplicitConstructor {
        private String str;

        public String getStr() {
            return str;
        }
    }

    private static class SimpleNoDefaultConstructor {
        private String str;

        public SimpleNoDefaultConstructor(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }

    private static class NullParamConstructor {
        private String str;

        public NullParamConstructor(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }

    private static class DefaultParamConstructor {
        private String str;

        public DefaultParamConstructor(String str) {
            if (str == null) {
                throw new NullPointerException();
            }
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }

    private static class NonDefaultParamConstructor {
        private String str;

        public NonDefaultParamConstructor(String str) {
            if (str == null) {
                throw new NullPointerException();
            } else if (str.isEmpty()) {
                throw new IllegalArgumentException();
            }
            this.str = str;
        }

        public String getStr() {
            return str;
        }
    }

    private static class ExceptionConstructor {
        private String str;

        public ExceptionConstructor(String str) {
            throw new IllegalArgumentException();
        }

        public String getStr() {
            return str;
        }
    }

    private static class NonSerializableConstructor implements Serializable {
        private String str;

        public NonSerializableConstructor(String str) {
            throw new IllegalArgumentException();
        }

        public String getStr() {
            return str;
        }
    }

}
