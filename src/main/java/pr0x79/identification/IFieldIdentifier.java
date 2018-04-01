package pr0x79.identification;

import java.lang.reflect.Field;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import pr0x79.exception.InstrumentorException;

/**
 * Identifies a field
 */
public interface IFieldIdentifier {
	public static final class FieldDescription {
		private final String name, desc;

		public FieldDescription(String name, String descriptor) {
			this.name = name;
			this.desc = descriptor;
		}

		public String getName() {
			return this.name;
		}

		public String getDescriptor() {
			return this.desc;
		}

		public Field reflect(Class<?> cls) {
			for(Field field : cls.getDeclaredFields()) {
				if(field.getName().equals(this.name) && Type.getDescriptor(field.getType()).equals(this.desc)) {
					return field;
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
			FieldDescription other = (FieldDescription) obj;
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
	 * Returns whether the field node matches
	 * @param field
	 * @return
	 */
	public default boolean isIdentifiedField(FieldNode field) {
		throw new InstrumentorException("Dynamic mapping not implemented");
	}

	/**
	 * Returns the fields' names and descriptors
	 * @return
	 */
	public default Set<FieldDescription> getFields() {
		throw new InstrumentorException("Static mapping not implemented");
	}

	/**
	 * Returns whether the identification is static ({@link #getFields()}) or dynamic ({@link #isIdentifiedField(FieldNode)})
	 * @return
	 */
	public boolean isStatic();
}
