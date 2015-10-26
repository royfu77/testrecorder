package com.almondtools.testrecorder;

import java.util.Collections;
import java.util.List;

public class DefaultConfig implements SnapshotConfig {

	@Override
	public MethodSnapshotConsumer getMethodConsumer() {
		return new TestGenerator();
	}
	
	@Override
	public ValueSnapshotConsumer getValueConsumer() {
		return new ValueGenerator();
	}

	@Override
	public long getTimeoutInMillis() {
		return 100_000;
	}

	@Override
	public List<String> getPackages() {
		return Collections.emptyList();
	}
}
