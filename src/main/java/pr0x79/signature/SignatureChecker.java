package pr0x79.signature;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.signature.SignatureVisitor;

import pr0x79.ClassHierarchy;
import pr0x79.ClassRelationResolver;
import pr0x79.signature.SignatureParser.FormalTypeParameterSymbol;
import pr0x79.signature.SignatureParser.TypeArgSymbol;
import pr0x79.signature.SignatureParser.TypeClassSymbol;
import pr0x79.signature.SignatureParser.TypeSymbol;
import pr0x79.signature.SignatureParser.TypeVariableSymbol;

/**
 * The signature checker checks whether a signature is compatible with another signature
 */
public class SignatureChecker {
	public static class TypeVariableResolvingException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1227880393715289564L;

		private final boolean isRequirement;
		private final TypeVariableSymbol typeVariable;

		public TypeVariableResolvingException(String msg, TypeVariableSymbol typeVariable, boolean isRequirement) {
			super(msg);
			this.isRequirement = isRequirement;
			this.typeVariable = typeVariable;
		}

		/**
		 * Whether the variable belongs to the requirement
		 * @return
		 */
		public boolean isRequirement() {
			return this.isRequirement;
		}

		/**
		 * The type variable that could not be resolved
		 * @return
		 */
		public TypeVariableSymbol getTypeVariable() {
			return this.typeVariable;
		}
	}

	protected final ClassHierarchy hierarchy;
	protected final ClassRelationResolver classRelationResolver;
	protected final ClassLoader loader;

	public SignatureChecker(ClassHierarchy hierarchy, ClassLoader loader) {
		this.hierarchy = hierarchy;
		this.loader = loader;
		this.classRelationResolver = new ClassRelationResolver(hierarchy, loader);
	}

	/**
	 * Returns whether a class <code>symbolCls</code> is compatible with the requirement class <code>requirementCls</code>.
	 * This method checks whether <code>{@literal <}? (extends|super) symbolCls{@literal >}</code> is compatible with <code>{@literal <}? (extends|super) requirementCls{@literal >}</code>.<p>
	 * Custom implementations can change this behaviour, for example a bidirectional signature check can check for class equality only instead of traversing the hierarchy.<p>
	 * The default implementation is equivalent to the following, with <code>symbolCls</code> and <code>requirementCls</code> swapped when <code>isSuper</code> is true:
	 * <p>
	 * <lu>
	 * <li>
	 * <code>symbolCls instanceof requirementCls</code>
	 * </li>
	 * <li>
	 * <code>requirementCls.isAssignableFrom(symbolCls)</code>
	 * </li>
	 * </lu>
	 * </p>
	 * It will always return false when <code>!isSuper && isKnownDowncast</code> is true because without further context downcasts cannot be allowed.<p>
	 * 
	 * @param requirementCls The internal name of the <code>requirementCls</code> class
	 * @param symbolCls The internal name of the <code>symbolCls</code> class
	 * @param includeInterfaces Whether <code>symbolCls</code> can be an interface
	 * @param isSuper Whether <code>symbolCls</code> must be a superclass of <code>requirementCls</code>
	 * @param isKnownDowncast True if <code>symbolCls</code> is already known to not be able to extend <code>requirementCls</code>, i.e. <code>symbolCls</code> is known to 
	 * be an interface and <code>requirementCls</code> is known to be a class. <i>Note:</i> <code>isKnownDowncast</code> may not always be true even if 
	 * <code>symbolCls</code> is actually an interface and <code>requirementCls</code> is a class, because that information cannot be determined
	 * from the class names only.
	 * @return
	 */
	protected boolean isCompatible(String requirementCls, String symbolCls, boolean includeInterfaces, boolean isSuper, boolean isKnownDowncast) {
		if(isSuper) {
			if("java/lang/Object".equals(symbolCls)) {
				return true; //Object will always be assignable to any <? super X>
			}
			return this.classRelationResolver.traverseHierarchy(requirementCls, (cls, itf, clsNode, flags) -> symbolCls.equals(cls), includeInterfaces);
		} else {
			if(isKnownDowncast) {
				//A downcast should never be allowed without further context
				return false;
			}
			if("java/lang/Object".equals(requirementCls)) {
				return true; //Any non-primitive class is always assignable to Object, no need to traverse hierarchy
			}
			return this.classRelationResolver.traverseHierarchy(symbolCls, (cls, itf, clsNode, flags) -> requirementCls.equals(cls), includeInterfaces);
		}
	}

	/**
	 * Returns whether the specified symbol suffices the specified requirement
	 * @param requirement The symbol that determines the requirement
	 * @param symbol The symbol that is to be checked whether it suffices the requirement
	 * @param requirementParameters The requirement's formal type parameters
	 * @param symbolParameters The symbol's formal type parameters
	 * @param checkArgs Whether type arguments should be checked
	 * @return
	 * @throws TypeVariableResolvingException Thrown if a type variable cannot be resolved
	 * @see SignatureTypesResolver
	 */
	public boolean check(TypeSymbol requirement, TypeSymbol symbol, 
			Map<String, FormalTypeParameterSymbol> requirementParameters, 
			Map<String, FormalTypeParameterSymbol> symbolParameters, 
			boolean checkArgs) throws TypeVariableResolvingException {
		return this.check(requirement, symbol,
				requirementParameters, symbolParameters,
				new HashSet<>(), new HashSet<>(),
				new HashSet<>(), new HashSet<>(),
				true, '/', true, checkArgs, false);
	}

	private boolean check(TypeSymbol requirement, TypeSymbol symbol,
			Map<String, FormalTypeParameterSymbol> requirementParameters,
			Map<String, FormalTypeParameterSymbol> symbolParameters,
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			boolean interfaces,
			char wildcard,
			boolean cycleFree,
			boolean checkArgs,
			boolean downcast) throws TypeVariableResolvingException {

		boolean requirementCycle = requirement.isVariable() && resolvedRequirementParameters.contains(requirement.getName());
		if(requirementCycle && !cycleFree && requirementCycleStart.isEmpty()) {
			requirementCycleStart.add(requirement.getName());
		}

		boolean symbolCycle = symbol.isVariable() && resolvedSymbolParameters.contains(symbol.getName());
		if(symbolCycle && !cycleFree && symbolCycleStart.isEmpty()) {
			symbolCycleStart.add(symbol.getName());
		}

		boolean isCurrentCycle = requirementCycle || symbolCycle;

		if(requirement.isVariable()) {
			resolvedRequirementParameters.add(requirement.getName());
		}

		if(symbol.isVariable()) {
			resolvedSymbolParameters.add(symbol.getName());
		}

		if(requirement.isVariable() && symbol.isVariable()) {
			FormalTypeParameterSymbol resolvedRequirementParameter = requirementParameters.get(requirement.getName());
			if(resolvedRequirementParameter == null) {
				throw new TypeVariableResolvingException(String.format("Requirement type variable %s could not be resolved", requirement.getName()), requirement.getAsVariable(), true);
			}
			FormalTypeParameterSymbol resolvedSymbolParameter = symbolParameters.get(symbol.getName());
			if(resolvedSymbolParameter == null) {
				throw new TypeVariableResolvingException(String.format("Symbol type variable %s could not be resolved", symbol.getName()), symbol.getAsVariable(), false);
			}
			return this.check(resolvedRequirementParameter, resolvedSymbolParameter,
					requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					wildcard, !isCurrentCycle && cycleFree, checkArgs, downcast);
		} else if(symbol.isVariable() && !requirement.isVariable()) {
			FormalTypeParameterSymbol resolvedSymbolParameter = symbolParameters.get(symbol.getName());
			if(resolvedSymbolParameter == null) {
				throw new TypeVariableResolvingException(String.format("Symbol type variable %s could not be resolved", symbol.getName()), symbol.getAsVariable(), false);
			}
			return this.check(requirement.getAsClass(), resolvedSymbolParameter,
					requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					wildcard, !isCurrentCycle && cycleFree, checkArgs, downcast);
		} else if(requirement.isVariable()) {
			FormalTypeParameterSymbol resolvedRequirementParameter = requirementParameters.get(requirement.getName());
			if(resolvedRequirementParameter == null) {
				throw new TypeVariableResolvingException(String.format("Requirement type variable %s could not be resolved", requirement.getName()), requirement.getAsVariable(), true);
			}
			return this.check(resolvedRequirementParameter, symbol.getAsClass(),
					requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					wildcard, !isCurrentCycle && cycleFree, checkArgs, downcast);
		} else {
			boolean isDowncast = downcast;
			downcast = false;
			switch(wildcard) {
			case '/':
			case SignatureVisitor.INSTANCEOF:
			case SignatureVisitor.EXTENDS:
				if(!this.isCompatible(requirement.getName(), symbol.getName(), interfaces, false, isDowncast)) {
					return false;
				}
				break;
			case SignatureVisitor.SUPER:
				//requirement class must be assignable to symbol, otherwise super is not satisfied
				if(!this.isCompatible(requirement.getName(), symbol.getName(), interfaces, true, isDowncast)) {
					return false;
				}
				break;
			}

			return !checkArgs || this.checkArgs(requirement.getAsClass(), symbol.getAsClass(),
					requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					cycleFree, checkArgs, downcast);
		}
	}

	private boolean checkArgs(TypeClassSymbol requirement, TypeClassSymbol symbol,
			Map<String, FormalTypeParameterSymbol> requirementParameters,
			Map<String, FormalTypeParameterSymbol> symbolParameters,
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			boolean cycleFree,
			boolean checkArgs,
			boolean downcast) throws TypeVariableResolvingException {
		List<TypeArgSymbol> requirementArgs = requirement.getArgs();
		List<TypeArgSymbol> symbolArgs = symbol.getArgs();

		if(requirementArgs.isEmpty()) {
			//No args at all in requirement -> any args are accepted
			return true;
		}

		if(requirementArgs.size() != symbolArgs.size()) {
			//Number of args does not match -> no way the symbol can suffice the requirement
			return false;
		}

		//Whether the args are from the first "layer", i.e. they are not an arg of an arg
		boolean outerArgs = resolvedRequirementParameters.isEmpty() || resolvedSymbolParameters.isEmpty();

		boolean suffices = true;

		//True if both the requirement and symbol have cycled in a loop at least once
		boolean hasDoubleCycle = !cycleFree && !requirementCycleStart.isEmpty() && !symbolCycleStart.isEmpty();

		args: for(int i = 0; i < requirementArgs.size(); i++) {
			TypeArgSymbol requirementArg = requirementArgs.get(i);
			TypeArgSymbol symbolArg = symbolArgs.get(i);

			//In double cycle -> skip previously visited variables
			if(hasDoubleCycle) {
				if(requirementArg.getSymbol().isVariable()) {
					if(requirementCycleStart.contains(requirementArg.getSymbol().getName())) {
						continue;
					}
				}

				if(symbolArg.getSymbol().isVariable()) {
					if(symbolCycleStart.contains(symbolArg.getSymbol().getName())) {
						continue;
					}
				}
			}

			switch(requirementArg.getWildcard()) {
			default:
			case '*':
				continue;
			case SignatureVisitor.INSTANCEOF: //instanceof can be treated the same way as extends
			case SignatureVisitor.EXTENDS:
				if(symbolArg.isAny() || symbolArg.isSuper()) {
					//This can never work because the symbol class will never have a lower bound
					suffices = false;
					break args;
				}
				if(!this.check(requirementArg.getSymbol(), symbolArg.getSymbol(), requirementParameters, symbolParameters,
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, symbolArg.getWildcard(), cycleFree, checkArgs, downcast)) {
					suffices = false;
					break args;
				}
				break;
			case SignatureVisitor.SUPER:
				if(symbolArg.isAny() || symbolArg.isExtends() || symbolArg.isSpecific()) {
					//This can never work because the symbol class will never have an upper bound
					suffices = false;
					break args;
				}
				if(!this.check(requirementArg.getSymbol(), symbolArg.getSymbol(), requirementParameters, symbolParameters,
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, symbolArg.getWildcard(), cycleFree, checkArgs, downcast)) {
					suffices = false;
					break args;
				}
				break;
			}
		}

		if(outerArgs) {
			resolvedRequirementParameters.clear();
			resolvedSymbolParameters.clear();
			requirementCycleStart.clear();
			symbolCycleStart.clear();
		}

		return suffices;
	}

	private boolean check(FormalTypeParameterSymbol requirement, TypeClassSymbol symbol,
			Map<String, FormalTypeParameterSymbol> requirementParameters,
			Map<String, FormalTypeParameterSymbol> symbolParameters,
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			char wildcard,
			boolean cycleFree,
			boolean checkArgs,
			boolean downcast) throws TypeVariableResolvingException {
		TypeSymbol requirementExtends = requirement.getExtends();
		if(requirementExtends != null) {
			if(!this.check(requirementExtends, symbol, requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					false, wildcard, cycleFree, checkArgs, downcast)) {
				return false;
			}
		}

		for(TypeSymbol requirementImplements : requirement.getImplements()) {
			if(!this.check(requirementImplements, symbol, requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					true, wildcard, cycleFree, checkArgs, downcast)) {
				return false;
			}
		}

		return true;
	}

	private boolean check(TypeClassSymbol requirement, FormalTypeParameterSymbol symbol,
			Map<String, FormalTypeParameterSymbol> requirementParameters,
			Map<String, FormalTypeParameterSymbol> symbolParameters,
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			char wildcard,
			boolean cycleFree,
			boolean checkArgs,
			boolean downcast) throws TypeVariableResolvingException {
		TypeSymbol symbolExtends = symbol.getExtends();
		if(symbolExtends != null) {
			if(this.check(requirement, symbolExtends, requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					false, wildcard, cycleFree, checkArgs, downcast)) {
				return true;
			}
		}

		for(TypeSymbol symbolImplements : symbol.getImplements()) {
			if(this.check(requirement, symbolImplements, requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					true, wildcard, cycleFree, checkArgs, downcast)) {
				return true;
			}
		}

		return false;
	}

	private boolean check(FormalTypeParameterSymbol requirement, FormalTypeParameterSymbol symbol,
			Map<String, FormalTypeParameterSymbol> requirementParameters,
			Map<String, FormalTypeParameterSymbol> symbolParameters,
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			char wildcard,
			boolean cycleFree,
			boolean checkArgs,
			boolean downcast) throws TypeVariableResolvingException {
		TypeSymbol requirementExtends = requirement.getExtends();
		TypeSymbol symbolExtends = symbol.getExtends();

		if(requirementExtends != null) {
			if(symbolExtends == null) {
				//Check if any of the symbol interfaces are allowed be downcast to the class that the requirement extends
				boolean suffices = false;
				for(TypeSymbol symbolImplements : symbol.getImplements()) {
					if(this.check(requirementExtends, symbolImplements,
							requirementParameters, symbolParameters,
							resolvedRequirementParameters, resolvedSymbolParameters, 
							requirementCycleStart, symbolCycleStart,
							true, wildcard, cycleFree, checkArgs, true)) {
						suffices = true;
						break;
					}
				}
				if(!suffices) {
					return false;
				}
			} else if(!this.check(requirementExtends, symbolExtends,
					requirementParameters, symbolParameters,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					false, wildcard, cycleFree, checkArgs, downcast)) {
				return false;
			}
		}

		for(TypeSymbol requirementImplements : requirement.getImplements()) {
			boolean suffices = false;
			if(symbolExtends != null) {
				//The class that the symbol extends might implement the requirement interface
				if(this.check(requirementImplements, symbolExtends,
						requirementParameters, symbolParameters,
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, wildcard, cycleFree, checkArgs, downcast)) {
					continue;
				}
			}
			for(TypeSymbol symbolImplements : symbol.getImplements()) {
				if(this.check(requirementImplements, symbolImplements,
						requirementParameters, symbolParameters,
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, wildcard, cycleFree, checkArgs, downcast)) {
					suffices = true;
					break;
				}
			}
			if(!suffices) {
				return false;
			}
		}

		return true;
	}
}
