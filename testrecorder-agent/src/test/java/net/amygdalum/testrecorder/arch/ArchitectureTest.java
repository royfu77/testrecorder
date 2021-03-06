package net.amygdalum.testrecorder.arch;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

public class ArchitectureTest {

	private static JavaClasses classes;

	@BeforeAll
	public static void beforeAll() throws Exception {
		classes = new ClassFileImporter()
			.importPath("target/classes");
	}

	@Test
	void testArchitecturalLayers() {
		layeredArchitecture()
			.layer("codeserializer").definedBy("net.amygdalum.testrecorder.codeserializer")
			.layer("callsiterecorder").definedBy("net.amygdalum.testrecorder.callsiterecorder")
			.layer("dynamiccompile").definedBy("net.amygdalum.testrecorder.dynamiccompile")
			.layer("data").definedBy("net.amygdalum.testrecorder.data")
			.layer("main").definedBy("net.amygdalum.testrecorder")
			.layer("profile").definedBy("net.amygdalum.testrecorder.profile")
			.layer("extensionpoint").definedBy("net.amygdalum.testrecorder.extensionpoint")
			.layer("serializers").definedBy("net.amygdalum.testrecorder.serializers")
			.layer("generator").definedBy("net.amygdalum.testrecorder.generator")
			.layer("deserializers").definedBy("net.amygdalum.testrecorder.deserializers")
			.layer("builder").definedBy("net.amygdalum.testrecorder.deserializers.builder")
			.layer("matcher").definedBy("net.amygdalum.testrecorder.deserializers.matcher")
			.layer("evaluator").definedBy("net.amygdalum.testrecorder.evaluator")
			.layer("values").definedBy("net.amygdalum.testrecorder.values")
			.layer("types").definedBy("net.amygdalum.testrecorder.types")
			.layer("util").definedBy("net.amygdalum.testrecorder.util")
			.layer("asm").definedBy("net.amygdalum.testrecorder.asm")

			.whereLayer("codeserializer").mayNotBeAccessedByAnyLayer() // only from outside module
			.whereLayer("callsiterecorder").mayNotBeAccessedByAnyLayer() // only from outside module
			.whereLayer("dynamiccompile").mayNotBeAccessedByAnyLayer() // only from outside module
			.whereLayer("data").mayNotBeAccessedByAnyLayer() // reserved for future use
			.whereLayer("main").mayOnlyBeAccessedByLayers(
				"codeserializer",
				"callsiterecorder",
				"generator")
			.whereLayer("profile").mayOnlyBeAccessedByLayers(
				"codeserializer",
				"callsiterecorder",
				"main",
				"generator",
				"builder",
				"matcher")
			.whereLayer("extensionpoint").mayOnlyBeAccessedByLayers(
				"main",
				"profile",
				"serializers",
				"generator",
				"builder",
				"matcher")
			.whereLayer("serializers").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder")
			.whereLayer("generator").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder")
			.whereLayer("deserializers").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"builder",
				"matcher",
				"generator")
			.whereLayer("builder").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"generator")
			.whereLayer("matcher").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"generator")
			.whereLayer("evaluator").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"generator")
			.whereLayer("values").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"generator",
				"serializers",
				"deserializers",
				"matcher",
				"builder",
				"evaluator")
			.whereLayer("types").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"profile",
				"generator",
				"serializers",
				"deserializers",
				"matcher",
				"builder",
				"evaluator",
				"values")
			.whereLayer("asm").mayOnlyBeAccessedByLayers(
				"codeserializer",
				"callsiterecorder",
				"profile",
				"serializers",
				"types",
				"values")
			.whereLayer("util").mayOnlyBeAccessedByLayers(
				"main",
				"codeserializer",
				"callsiterecorder",
				"profile",
				"generator",
				"serializers",
				"deserializers",
				"matcher",
				"builder",
				"evaluator",
				"values",
				"types",
				"asm")
			.check(classes);
	}

	@Test
	void testNoCycles() {
		slices()
			.matching("net.amygdalum.(**)")
			.should().beFreeOfCycles()
			.check(classes);
	}
}
