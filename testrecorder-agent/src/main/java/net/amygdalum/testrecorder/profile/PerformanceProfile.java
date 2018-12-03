package net.amygdalum.testrecorder.profile;

import net.amygdalum.testrecorder.ExtensionPoint;
import net.amygdalum.testrecorder.ExtensionStrategy;

@ExtensionPoint(strategy=ExtensionStrategy.OVERRIDING)
public interface PerformanceProfile {

	long getTimeoutInMillis();

	long getIdleTime();

}
