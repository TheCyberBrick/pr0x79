package pr0x79.instrumentation.accessor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Interceptors {
	private final Map<String, MethodInterceptorData> interceptors;

	public Interceptors(Map<String, MethodInterceptorData> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * Returns a collection of all interceptors
	 * @return
	 */
	public Collection<MethodInterceptorData> getMethodInterceptors() {
		return Collections.unmodifiableCollection(this.interceptors.values());
	}
}
