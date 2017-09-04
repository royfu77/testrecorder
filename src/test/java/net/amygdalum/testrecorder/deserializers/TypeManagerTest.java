package net.amygdalum.testrecorder.deserializers;

import static net.amygdalum.testrecorder.util.Types.array;
import static net.amygdalum.testrecorder.util.Types.parameterized;
import static net.amygdalum.testrecorder.util.Types.wildcard;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TypeManagerTest {

    private TypeManager types;

    @Before
    public void before() throws Exception {
        types = new TypeManager("net.amygdalum.testrecorder.deserializers");
    }

    @Test
    public void testGetPackage() throws Exception {
        assertThat(types.getPackage(), equalTo("net.amygdalum.testrecorder.deserializers"));
        assertThat(new TypeManager().getPackage(), equalTo(""));
    }

    @Test
    public void testRegisterTypes() throws Exception {
        types.registerTypes(Integer.class, List.class);

        assertThat(types.getImports(), containsInAnyOrder("java.util.List", "java.lang.Integer"));
    }

    @Test
    public void testStaticImport() throws Exception {
        types.staticImport(Collections.class, "sort");

        assertThat(types.getImports(), contains("static java.util.Collections.sort"));
    }

    @Test
    public void testRegisterTypeArray() throws Exception {
        types.registerType(array(parameterized(List.class, null, String.class)));

        assertThat(types.getImports(), containsInAnyOrder("java.lang.String", "java.util.List"));
    }

    @Test
    public void testRegisterTypeOther() throws Exception {
        types.registerType(mock(Type.class));

        assertThat(types.getImports(), empty());
    }

    @Test
    public void testRegisterImport() throws Exception {
        types.registerImport(String.class);

        assertThat(types.getImports(), contains("java.lang.String"));
    }

    @Test
    public void testRegisterImportPrimitive() throws Exception {
        types.registerImport(int.class);

        assertThat(types.getImports(), empty());
    }

    @Test
    public void testRegisterImportArray() throws Exception {
        types.registerImport(Integer[].class);

        assertThat(types.getImports(), contains("java.lang.Integer"));
    }

    @Test
    public void testRegisterImportCached() throws Exception {
        types.registerImport(String.class);
        types.registerImport(String.class);

        assertThat(types.getImports(), contains("java.lang.String"));
    }

    @Test
    public void testRegisterImportColliding() throws Exception {
        types.registerImport(StringTokenizer.class);
        types.registerImport(java.util.StringTokenizer.class);

        assertThat(types.getImports(), contains("net.amygdalum.testrecorder.deserializers.StringTokenizer"));
    }

    @Test
    public void testRegisterImportHidden() throws Exception {
        types.registerImport(Hidden.class);

        assertThat(types.getImports(), containsInAnyOrder("net.amygdalum.testrecorder.runtime.Wrapped", "static net.amygdalum.testrecorder.runtime.Wrapped.clazz"));
    }

    @Test
    public void testRegisterImportHiddenCached() throws Exception {
        types.registerImport(Hidden.class);
        types.registerImport(Hidden.class);

        assertThat(types.getImports(), containsInAnyOrder("net.amygdalum.testrecorder.runtime.Wrapped", "static net.amygdalum.testrecorder.runtime.Wrapped.clazz"));
    }

    @Test
    public void testIsHiddenType() throws Exception {
        assertThat(types.isHidden(Hidden.class), is(true));
    }

    @Test
    public void testIsHiddenConstructor() throws Exception {
        assertThat(types.isHidden(Hidden.class.getDeclaredConstructor()), is(true));
    }

    @Test
    public void testGetVariableTypeNameWithoutImport() throws Exception {
        assertThat(types.getVariableTypeName(List.class), equalTo("java.util.List<?>"));
    }

    @Test
    public void testGetVariableTypeNameWithImport() throws Exception {
        types.registerType(String.class);

        assertThat(types.getVariableTypeName(String.class), equalTo("String"));
    }

    @Test
    public void testGetVariableTypeNameOfArray() throws Exception {
        types.registerType(String.class);

        assertThat(types.getVariableTypeName(String[].class), equalTo("String[]"));
    }

    @Test
    public void testGetVariableTypeNameOfGenericArray() throws Exception {
        types.registerType(List.class);
        types.registerType(String.class);

        assertThat(types.getVariableTypeName(array(parameterized(List.class, null, String.class))), equalTo("List<String>[]"));
        assertThat(types.getVariableTypeName(array(parameterized(List.class, null, Date.class))), equalTo("List<java.util.Date>[]"));
        assertThat(types.getVariableTypeName(array(List.class)), equalTo("List<?>[]"));
    }

    @Test
    public void testGetVariableTypeNameGenericWithImport() throws Exception {
        types.registerType(List.class);
        types.registerType(Map.class);

        assertThat(types.getVariableTypeName(List.class), equalTo("List<?>"));
        assertThat(types.getVariableTypeName(Map.class), equalTo("Map<?, ?>"));
        assertThat(types.getConstructorTypeName(parameterized(List.class, null, parameterized(List.class, null, wildcard()))), equalTo("List<List<?>>"));
        assertThat(types.getVariableTypeName(parameterized(List.class, null, String.class)), equalTo("List<String>"));
        assertThat(types.getVariableTypeName(parameterized(List.class, null, Date.class)), equalTo("List<java.util.Date>"));
    }

    @Test
    public void testGetVariableTypeNameNestedType() throws Exception {
        assertThat(types.getVariableTypeName(net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface.class), equalTo("net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface"));
    }

    @Test
    public void testGetVariableTypeNameOther() throws Exception {
        assertThat(types.getVariableTypeName(mock(Type.class)), equalTo("Object"));
    }

    @Test
    public void testGetConstructorTypeNameWithoutImport() throws Exception {
        assertThat(types.getConstructorTypeName(List.class), equalTo("java.util.List<>"));
    }
    
    @Test
    public void testGetConstructorTypeNameWithImport() throws Exception {
        types.registerType(String.class);
        
        assertThat(types.getConstructorTypeName(String.class), equalTo("String"));
    }
    
    @Test
    public void testGetConstructorTypeNameOfArray() throws Exception {
        types.registerType(String.class);
        
        assertThat(types.getConstructorTypeName(String[].class), equalTo("String[]"));
    }
    
    @Test
    public void testGetConstructorTypeNameOfGenericArray() throws Exception {
        types.registerType(List.class);
        types.registerType(String.class);
        
        assertThat(types.getConstructorTypeName(array(parameterized(List.class, null, String.class))), equalTo("List<String>[]"));
        assertThat(types.getConstructorTypeName(array(parameterized(List.class, null, Date.class))), equalTo("List<java.util.Date>[]"));
        assertThat(types.getConstructorTypeName(array(List.class)), equalTo("List<>[]"));
    }
    
    @Test
    public void testGetConstructorTypeNameGenericWithImport() throws Exception {
        types.registerType(List.class);
        types.registerType(Map.class);

        assertThat(types.getConstructorTypeName(List.class), equalTo("List<>"));
        assertThat(types.getConstructorTypeName(Map.class), equalTo("Map<>"));
        assertThat(types.getConstructorTypeName(parameterized(List.class, null, parameterized(List.class, null, wildcard()))), equalTo("List<List<?>>"));
        assertThat(types.getConstructorTypeName(parameterized(List.class, null, String.class)), equalTo("List<String>"));
        assertThat(types.getConstructorTypeName(parameterized(List.class, null, Date.class)), equalTo("List<java.util.Date>"));
    }
    
    @Test
    public void testGetConstructorTypeNameOther() throws Exception {
        assertThat(types.getConstructorTypeName(mock(Type.class)), equalTo("Object"));
    }
   
    @Test
    public void testGetConstructorTypeNameNestedType() throws Exception {
        assertThat(types.getConstructorTypeName(net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface.class), equalTo("net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface"));
    }

    @Test
    public void testGetRawTypeNameNestedType() throws Exception {
        assertThat(types.getRawTypeName(net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface.class), equalTo("net.amygdalum.testrecorder.util.testobjects.Hidden.VisibleInterface"));
    }

    private static class Hidden {

    }

}

class StringTokenizer {

}