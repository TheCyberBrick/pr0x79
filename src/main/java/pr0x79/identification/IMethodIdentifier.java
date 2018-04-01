package pr0x79.identification;

import java.lang.reflect.Method;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import pr0x79.exception.InstrumentorException;

/**
 * Identifies a method
 */
public interface IMethodIdentifier {
	public static final class MethodDescription {
		private final String name, desc;

		public MethodDescription(String name, String descriptor) {
			this.name = name;
			this.desc = descriptor;
		}

		public String getName() {
			return this.name;
		}

		public String getDescriptor() {
			return this.desc;
		}

		public Method reflect(Class<?> cls) {
			for(Method method : cls.getDeclaredMethods()) {
				if(method.getName().equals(this.name) && Type.getMethodDescriptor(method).equals(this.desc)) {
					return method;
				}
			}
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((desc == null) ? 0 : desc.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MethodDescription other = (MethodDescription) obj;
			if (desc == null) {
				if (other.desc != null)
					return false;
			} else if (!desc.equals(other.desc))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	/**
	 * Returns whether the method node matches
	 * @param method
	 * @return
	 */
	public default boolean isIdentifiedMethod(MethodNode method) {
		throw new InstrumentorException("Dynamic mapping not implemented");
	}

	/**
	 * Returns the methods' names and descriptors
	 * @return
	 */
	public default Set<MethodDescription> getMethods() {
		throw new InstrumentorException("Static mapping not implemented");
	}

	/**
	 * Returns whether the identification is static ({@link #getMethods()}) or dynamic ({@link #isIdentifiedMethod(MethodNode)})
	 * @return
	 */
	public boolean isStatic();
}
