package pr0x79.instrumentation.signature;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class SignatureParser {
	/**
	 * Parses the specified signature
	 * @param signature
	 * @return
	 */
	public static Signature parse(String signature) {
		SignatureParserVisitor vis = new SignatureParserVisitor(Opcodes.ASM5);

		new SignatureReader(signature).accept(vis);

		Signature sig = new Signature();

		sig.formalTypeParameters.addAll(vis.getFormalTypeParameters());
		sig.parameters.addAll(vis.getParameterTypes());
		sig.returnType = vis.getReturnType();
		sig.exceptions.addAll(vis.getExceptions());
		sig.superclass = vis.getSuperclass();
		sig.interfaces.addAll(vis.getInterfaces());

		return sig;
	}

	/**
	 * Generic symbol used in the signature
	 */
	public static abstract class SignatureSymbol {
		private SignatureSymbol() { }

		/**
		 * The name of this symbol
		 * @return
		 */
		public abstract String getName();

		/**
		 * Accepts the signature visitor
		 * @param visitor
		 */
		public abstract void accept(SignatureVisitor visitor);

		/**
		 * Traverses all child symbols in depth first order
		 * @param consumer
		 */
		public abstract void traverseDFS(Consumer<SignatureSymbol> consumer);
	}

	/**
	 * Generic symbol that describes a type
	 */
	public static abstract class TypeSymbol extends SignatureSymbol { 
		private TypeSymbol() { }

		/**
		 * Whether this type symbol is a variable
		 * @return
		 */
		public abstract boolean isVariable();

		/**
		 * Whether this type symbol is an array
		 * @return
		 */
		public abstract boolean isArray();

		/**
		 * Casts this type to a variable type
		 * @see #isVariable()
		 * @return
		 */
		public TypeVariableSymbol getAsVariable() {
			return (TypeVariableSymbol) this;
		}

		/**
		 * Casts this type to a class type
		 * @see #isVariable()
		 * @return
		 */
		public TypeClassSymbol getAsClass() {
			return (TypeClassSymbol) this;
		}
	}

	/**
	 * Describes a class type
	 */
	public static class TypeClassSymbol extends TypeSymbol {
		private Type type;
		private boolean array;
		private List<TypeArgSymbol> args = new ArrayList<>();

		private TypeClassSymbol() {}

		public TypeClassSymbol(Type type, boolean array, List<TypeArgSymbol> args) {
			this.type = type;
			this.array = array;
			this.args = args;
		}

		/**
		 * Returns the type of the symbol
		 * @return
		 */
		public Type getType() {
			return this.type;
		}

		@Override
		public String getName() {
			return this.type.getInternalName();
		}

		/**
		 * Returns the arguments of this type
		 * @return
		 */
		public List<TypeArgSymbol> getArgs() {
			return this.args;
		}

		@Override
		public boolean isVariable() {
			return false;
		}

		@Override
		public boolean isArray() {
			return this.array;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.type.getClassName());
			if(!this.args.isEmpty()) {
				sb.append("<");
				StringJoiner joiner = new StringJoiner(", ");
				for(TypeArgSymbol arg : this.args) {
					joiner.add(arg.toString());
				}
				sb.append(joiner.toString());
				sb.append(">");
			}
			if(this.isArray()) {
				sb.append("[]");
			}
			return sb.toString();
		}

		@Override
		public void accept(SignatureVisitor visitor) {
			String[] classes = this.getName().split("$");
			if(this.isArray()) {
				visitor = visitor.visitArrayType();
			}
			if(this.type.getSort() == Type.OBJECT) {
				visitor.visitClassType(classes[0]);
				for(int i = 1; i < classes.length; i++) {
					visitor.visitInnerClassType(classes[i]);
				}
				for(TypeArgSymbol arg : this.args) {
					arg.accept(visitor);
				}
				visitor.visitEnd();
			} else {
				visitor.visitBaseType(this.type.getDescriptor().charAt(0));
			}
		}

		@Override
		public void traverseDFS(Consumer<SignatureSymbol> consumer) {
			for(TypeArgSymbol arg : this.args) {
				consumer.accept(arg);
				arg.traverseDFS(consumer);
			}
		}
	}

	/**
	 * Describes a class type argument
	 */
	public static class TypeArgSymbol extends SignatureSymbol {
		private char wildcard;
		private TypeSymbol symbol;

		private TypeArgSymbol() {}

		public TypeArgSymbol(char wildcard, TypeSymbol symbol) {
			this.wildcard = wildcard;
			this.symbol = symbol;
		}

		@Override
		public String getName() {
			return String.valueOf(this.wildcard);
		}

		/**
		 * Returns the wildcard of the arg.
		 * Can be the following characters:
		 * <ul>
		 * <li>{@link SignatureVisitor#EXTENDS}</li>
		 * <li>{@link SignatureVisitor#SUPER}</li>
		 * <li>{@link SignatureVisitor#INSTANCEOF}</li>
		 * <li>*</li>
		 * </ul>
		 * @return
		 */
		public char getWildcard() {
			return this.wildcard;
		}

		/**
		 * Returns the symbol of the arg. Can be null.<p>
		 * Can be {@link TypeClassSymbol} or {@link TypeVariableSymbol}
		 * @return
		 */
		public TypeSymbol getSymbol() {
			return this.symbol;
		}

		/**
		 * Whether this argument is equivalent to {@literal <?>}
		 * @return
		 */
		public boolean isAny() {
			return this.wildcard == '*';
		}

		/**
		 * Whether this argument is equivalent to {@literal <? extends }<i>symbol</>{@literal >}
		 * @return
		 */
		public boolean isExtends() {
			return this.wildcard == SignatureVisitor.EXTENDS;
		}

		/**
		 * Whether this argument is equivalent to {@literal <? super }<i>symbol</>{@literal >}
		 * @return
		 */
		public boolean isSuper() {
			return this.wildcard == SignatureVisitor.SUPER;
		}

		/**
		 * Whether this argument is equivalent to {@literal <}<i>symbol</>{@literal >}
		 * @return
		 */
		public boolean isSpecific() {
			return this.wildcard == SignatureVisitor.INSTANCEOF;
		}

		@Override
		public String toString() {
			switch(this.wildcard) {
			default:
			case '*':
				return "?";
			case SignatureVisitor.EXTENDS:
				return "? extends " + this.getSymbol().toString();
			case SignatureVisitor.SUPER:
				return "? super " + this.getSymbol().toString();
			case SignatureVisitor.INSTANCEOF:
				return this.getSymbol().toString();
			}
		}

		@Override
		public void accept(SignatureVisitor visitor) {
			if(this.isAny()) {
				visitor.visitTypeArgument();
			} else {
				this.getSymbol().accept(visitor.visitTypeArgument(this.wildcard));
			}
		}

		@Override
		public void traverseDFS(Consumer<SignatureSymbol> consumer) {
			if(this.symbol != null) {
				consumer.accept(this.symbol);
				this.symbol.traverseDFS(consumer);
			}
		}
	}

	/**
	 * Describes a variable type
	 */
	public static class TypeVariableSymbol extends TypeSymbol {
		private String name;
		private boolean array;

		private TypeVariableSymbol() {}

		public TypeVariableSymbol(String name, boolean array) {
			this.name = name;
			this.array = array;
		}

		/**
		 * Returns the name of the type variable
		 * @return
		 */
		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean isVariable() {
			return true;
		}

		@Override
		public boolean isArray() {
			return this.array;
		}

		@Override
		public String toString() {
			return this.getName() + (this.isArray() ? "[]" : "");
		}

		@Override
		public void accept(SignatureVisitor visitor) {
			if(this.isArray()) {
				visitor = visitor.visitArrayType();
			}
			visitor.visitTypeVariable(this.getName());
		}

		@Override
		public void traverseDFS(Consumer<SignatureSymbol> consumer) { }
	}

	/**
	 * Describes a formal type parameter
	 */
	public static class FormalTypeParameterSymbol extends SignatureSymbol {
		private String name;
		private TypeSymbol extendsType;
		private List<TypeClassSymbol> implementsTypes = new ArrayList<>();

		private FormalTypeParameterSymbol() {}

		/**
		 * Returns the name of the type parameter
		 * @return
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the symbol that this parameter must extend.
		 * Can be {@link TypeClassSymbol} or {@link TypeVariableSymbol}
		 * @return
		 */
		public TypeSymbol getExtends() {
			return this.extendsType;
		}

		/**
		 * Returns the symbols that this parameter must implement
		 * @return
		 */
		public List<TypeClassSymbol> getImplements() {
			return this.implementsTypes;
		}

		@Override
		public String toString() {
			List<TypeSymbol> allExtends = new ArrayList<>();
			if(this.getExtends() != null) {
				allExtends.add(this.getExtends());
			}
			allExtends.addAll(this.getImplements());
			StringBuilder sb = new StringBuilder();
			sb.append(this.name);
			if(!allExtends.isEmpty()) {
				StringJoiner joiner = new StringJoiner(" & ");
				for(TypeSymbol type : allExtends) {
					joiner.add(type.toString());
				}
				sb.append(" extends " + joiner.toString());
			}
			return sb.toString();
		}

		@Override
		public void accept(SignatureVisitor visitor) {
			visitor.visitFormalTypeParameter(this.name);
			if(this.extendsType != null) {
				this.extendsType.accept(visitor.visitClassBound());
			}
			for(TypeClassSymbol impl : this.implementsTypes) {
				impl.accept(visitor.visitInterfaceBound());
			}
		}

		@Override
		public void traverseDFS(Consumer<SignatureSymbol> consumer) {
			if(this.extendsType != null) {
				consumer.accept(this.extendsType);
				this.extendsType.traverseDFS(consumer);
			}
			for(TypeClassSymbol itf : this.implementsTypes) {
				consumer.accept(itf);
				itf.traverseDFS(consumer);
			}
		}
	}

	/**
	 * Describes a full signature
	 */
	public static class Signature {
		public final List<FormalTypeParameterSymbol> formalTypeParameters = new ArrayList<>();
		public final List<TypeSymbol> parameters = new ArrayList<>();
		public final List<TypeSymbol> interfaces = new ArrayList<>();
		public final List<TypeSymbol> exceptions = new ArrayList<>();
		public TypeSymbol returnType;
		public TypeSymbol superclass;

		/**
		 * Accepts the signature visitor
		 * @param visitor
		 */
		public void accept(SignatureVisitor visitor) {
			for(FormalTypeParameterSymbol formalType : this.formalTypeParameters) {
				formalType.accept(visitor);
			}
			for(TypeSymbol param : this.parameters) {
				param.accept(visitor.visitParameterType());
			}
			if(this.returnType != null) {
				this.returnType.accept(visitor.visitReturnType());
			}
			for(TypeSymbol excp : this.exceptions) {
				excp.accept(visitor.visitExceptionType());
			}
			if(this.superclass != null) {
				this.superclass.accept(visitor.visitSuperclass());
			}
			for(TypeSymbol itf : this.interfaces) {
				itf.accept(visitor.visitInterface());
			}
		}

		/**
		 * Traverses all symbols in depth first order
		 * @param consumer
		 */
		public void traverseDFS(Consumer<SignatureSymbol> consumer) {
			for(FormalTypeParameterSymbol formalType : this.formalTypeParameters) {
				consumer.accept(formalType);
				formalType.traverseDFS(consumer);
			}
			for(TypeSymbol param : this.parameters) {
				consumer.accept(param);
				param.traverseDFS(consumer);
			}
			if(this.returnType != null) {
				consumer.accept(this.returnType);
				this.returnType.traverseDFS(consumer);
			}
			for(TypeSymbol excp : this.exceptions) {
				consumer.accept(excp);
				excp.traverseDFS(consumer);
			}
			if(this.superclass != null) {
				consumer.accept(this.superclass);
				this.superclass.traverseDFS(consumer);
			}
			for(TypeSymbol itf : this.interfaces) {
				consumer.accept(itf);
				itf.traverseDFS(consumer);
			}
		}
	}

	public static class SignatureParserVisitor extends SignatureVisitor {
		private final List<FormalTypeParameterSymbol> formal = new ArrayList<>();
		private final List<TypeSymbol> params = new ArrayList<>();
		private final List<TypeSymbol> interfaces = new ArrayList<>();
		private final List<TypeSymbol> exceptions = new ArrayList<>();
		private TypeSymbol ret;
		private TypeSymbol superclass;

		private FormalTypeParameterSymbol typeParam;

		/**
		 * This signature visitor parses a signature into an ASM Tree API-like structure
		 * @param api
		 */
		public SignatureParserVisitor(int api) {
			super(api);
		}

		/**
		 * Returns all formal type parameters
		 * @return
		 */
		public List<FormalTypeParameterSymbol> getFormalTypeParameters() {
			return this.formal;
		}

		/**
		 * Returns all method parameter types
		 * @return
		 */
		public List<TypeSymbol> getParameterTypes() {
			return this.params;
		}

		/**
		 * Returns the method return type
		 * @return
		 */
		public TypeSymbol getReturnType() {
			return this.ret;
		}

		/**
		 * Returns all interfaces the class implements
		 * @return
		 */
		public List<TypeSymbol> getInterfaces() {
			return this.interfaces;
		}

		/**
		 * Returns all exceptions the method throws
		 * @return
		 */
		public List<TypeSymbol> getExceptions() {
			return this.exceptions;
		}

		/**
		 * Returns the superclass of the class
		 * @return
		 */
		public TypeSymbol getSuperclass() {
			return this.superclass;
		}

		@Override
		public void visitFormalTypeParameter(String name) {
			this.typeParam = new FormalTypeParameterSymbol();
			this.typeParam.name = name;
			this.formal.add(this.typeParam);
		}

		@Override
		public SignatureVisitor visitClassBound() {
			return new SignatureTypeVisitor(this.api, this.typeParam, false);
		}

		@Override
		public SignatureVisitor visitInterfaceBound() {
			return new SignatureTypeVisitor(this.api, this.typeParam, true);
		}

		@Override
		public SignatureVisitor visitParameterType() {
			return new SignatureTypeVisitor(this.api, param -> this.params.add(param));
		}

		@Override
		public SignatureVisitor visitReturnType() {
			return new SignatureTypeVisitor(this.api, ret -> this.ret = ret);
		}

		@Override
		public SignatureVisitor visitSuperclass() {
			return new SignatureTypeVisitor(this.api, cls -> this.superclass = cls);
		}

		@Override
		public SignatureVisitor visitInterface() {
			return new SignatureTypeVisitor(this.api, itf -> this.interfaces.add(itf));
		}

		@Override
		public SignatureVisitor visitExceptionType() {
			return new SignatureTypeVisitor(this.api, excp -> this.exceptions.add(excp));
		}
	}

	public static class SignatureTypeVisitor extends SignatureVisitor {
		private final Consumer<TypeSymbol> types;

		private final TypeArgSymbol argSymbol;

		private final FormalTypeParameterSymbol paramSymbol;
		private final boolean paramIface;

		private TypeClassSymbol typeSymbol;

		private boolean array = false;

		/**
		 * Creates a signature type visitor to parse any signature types
		 * except formal type parameters or type wildcards
		 * @param api ASM API version
		 * @param types Accepts new type before it is being visited
		 */
		public SignatureTypeVisitor(int api, Consumer<TypeSymbol> types) {
			super(api);
			this.types = types;
			this.argSymbol = null;
			this.paramSymbol = null;
			this.paramIface = false;
		}

		/**
		 * Creates a signature type visitor to parse a formal type parameter
		 * @param api ASM API version
		 * @param symbol The formal type parameter that is being visited
		 * @param iface Whether a class bound or interface bound is being visited
		 */
		public SignatureTypeVisitor(int api, FormalTypeParameterSymbol symbol, boolean iface) {
			super(api);
			this.types = null;
			this.argSymbol = null;
			this.paramSymbol = symbol;
			this.paramIface = iface;
		}

		/**
		 * Creates a signature type visitor to parse a type argument
		 * @param api ASM API version
		 * @param symbol The type arg that is being visited
		 */
		private SignatureTypeVisitor(int api, TypeArgSymbol symbol) {
			super(api);
			this.types = null;
			this.argSymbol = symbol;
			this.paramSymbol = null;
			this.paramIface = false;
		}

		@Override
		public SignatureVisitor visitArrayType() {
			this.array = true;
			return this;
		}

		@Override
		public void visitTypeVariable(String name) {
			TypeVariableSymbol var = new TypeVariableSymbol();
			var.name = name;
			var.array = this.array;
			this.array = false;
			if(this.types != null) this.types.accept(var);
			if(this.argSymbol != null) this.argSymbol.symbol = var;
			if(this.paramSymbol != null) this.paramSymbol.extendsType = var;
			//Type variables cannot have arguments, no need to propagate through this.typeSymbol
		}

		@Override
		public void visitClassType(String name) {
			TypeClassSymbol type = new TypeClassSymbol();
			type.type = Type.getObjectType(name);
			type.array = this.array;
			this.array = false;
			this.typeSymbol = type;
			if(this.types != null) this.types.accept(type);
			if(this.argSymbol != null) this.argSymbol.symbol = type;
			if(this.paramSymbol != null) {
				if(this.paramIface) {
					this.paramSymbol.implementsTypes.add(this.typeSymbol);
				} else {
					this.paramSymbol.extendsType = this.typeSymbol;
				}
			}
		}

		@Override
		public void visitInnerClassType(String name) {
			//Append inner class name
			this.typeSymbol.type = Type.getObjectType(this.typeSymbol.type.getInternalName() + "$" + name);
		}

		@Override
		public void visitBaseType(char descriptor) {
			TypeClassSymbol type = new TypeClassSymbol();
			type.type = Type.getType(String.valueOf(descriptor));
			type.array = this.array;
			this.array = false;
			this.typeSymbol = type;
			if(this.types != null) this.types.accept(type);
			if(this.argSymbol != null) this.argSymbol.symbol = type;
		}

		@Override
		public void visitTypeArgument() {
			TypeArgSymbol arg = new TypeArgSymbol();
			arg.wildcard = '*';
			this.typeSymbol.args.add(arg);
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			TypeArgSymbol arg = new TypeArgSymbol();
			arg.wildcard = wildcard;
			this.typeSymbol.args.add(arg);
			return new SignatureTypeVisitor(this.api, arg);
		}
	}
}
