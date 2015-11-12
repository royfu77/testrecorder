package com.almondtools.testrecorder.data;

public class RandomIntValueGenerator extends RandomValueGenerator<Integer> {
	
	@Override
	public Integer create(TestDataGenerator generator) {
		return random.nextInt();
	}

}
