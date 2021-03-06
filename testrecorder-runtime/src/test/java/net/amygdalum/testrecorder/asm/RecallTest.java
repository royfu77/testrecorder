package net.amygdalum.testrecorder.asm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

public class RecallTest {

	private MethodContext context;

	@BeforeEach
	void before() {
		context = new MethodContext(AClass.classNode(), AClass.staticMethodNode());
	}

	@Test
	void testRecall() throws Exception {
		Local local = context.newLocal("x", Type.getType(long.class));
		
		InsnList insns = new Recall("x")
			.build(context);

		assertThat(ByteCode.toString(insns))
			.containsExactly(
				"LLOAD " + local.index);
	}

}
