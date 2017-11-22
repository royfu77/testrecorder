package net.amygdalum.testrecorder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class AllLambdasSerializableTransformer implements ClassFileTransformer {

	private static final int IS_SERIALIZABLE_PARAMETER_LOCAL = 7;

	public static AllLambdasSerializableTransformer INSTANCE = new AllLambdasSerializableTransformer();

	private AllLambdasSerializableTransformer() {
	}

	public Class<?>[] classesToRetransform() throws ClassNotFoundException {
		return new Class[] { Class.forName("java.lang.invoke.InnerClassLambdaMetafactory") };
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (className != null && className.equals("java/lang/invoke/InnerClassLambdaMetafactory")) {
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassNode classNode = new ClassNode();

			cr.accept(classNode, 0);
			classNode.methods.stream()
				.filter(method -> "<init>".equals(method.name))
				.findFirst()
				.ifPresent(method -> {
					VarInsnNode serialized = findIsSerializeableLocalVariable(method);
					method.instructions.set(serialized, new InsnNode(Opcodes.ICONST_1));
				});

			ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(out);
			return out.toByteArray();
		}
		return null;
	}

	private VarInsnNode findIsSerializeableLocalVariable(MethodNode method) {
		return stream(method.instructions.iterator())
			.filter(node -> node instanceof VarInsnNode)
			.map(node -> (VarInsnNode) node)
			.filter(node -> node.var == IS_SERIALIZABLE_PARAMETER_LOCAL)
			.findFirst()
			.orElse(null);
	}

	private <T> Stream<T> stream(Iterator<T> iterator) {
		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
		return StreamSupport.stream(spliterator, false);
	}

}