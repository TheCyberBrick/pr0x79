package pr0x79.signature;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.signature.SignatureVisitor;

import pr0x79.ClassRelationResolver;
import pr0x79.signature.SignatureParser.FormalTypeParameterSymbol;
import pr0x79.signature.SignatureParser.TypeArgSymbol;
import pr0x79.signature.SignatureParser.TypeClassSymbol;
import pr0x79.signature.SignatureParser.TypeSymbol;
import pr0x79.signature.SignatureParser.TypeVariableSymbol;

public class SignatureCompatibilityChecker {
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
	protected final Map<String, FormalTypeParameterSymbol> requirementParameters;
	protected final Map<String, FormalTypeParameterSymbol> symbolParameters;

	public SignatureCompatibilityChecker(
			ClassHierarchy hierarchy,
			ClassLoader loader,
			Map<String, FormalTypeParameterSymbol> requirementParameters, 
			Map<String, FormalTypeParameterSymbol> symbolParameters) {
		this.hierarchy = hierarchy;
		this.classRelationResolver = new ClassRelationResolver(hierarchy, loader);
		this.requirementParameters = requirementParameters;
		this.symbolParameters = symbolParameters;
	}

	/**
	 * Returns whether a class <code>symbolCls</code> is compatible with the requirement class <code>requirementCls</code>.
	 * This method checks whether <code>{@literal <}? (extends|super) symbolCls{@literal >}</code> is compatible with <code>{@literal <}? (extends|super) requirementCls{@literal >}</code>.<p>
	 * Custom implementations can change this behaviour, for example a bidirectional signature check can check for class equality only instead of traversing the hierarchy.<p>
	 * The default implementation is equivalent to the following, with <code>symbolCls</code> and <code>requirementCls</code> swapped when <code>isSuper</code> is true:
	 * <lu>
	 * <li>
	 * <code>symbolCls instanceof requirementCls</code>
	 * </li>
	 * <li>
	 * <code>requirementCls.isAssignableFrom(symbolCls)</code>
	 * </li>
	 * </lu>
	 * @param requirementCls The internal name of the <code>requirementCls</code> class
	 * @param symbolCls The internal name of the <code>symbolCls</code> class
	 * @param includeInterfaces Whether <code>symbolCls</code> can be an interface
	 * @param isSuper Whether <code>symbolCls</code> must be a superclass of <code>requirementCls</code>
	 * @return
	 */
	protected boolean isCompatible(String requirementCls, String symbolCls, boolean includeInterfaces, boolean isSuper) {
		if(isSuper) {
			if("java/lang/Object".equals(symbolCls)) {
				return true; //Object will always be assignable to any <? super X>
			}
			return this.classRelationResolver.traverseHierarchy(requirementCls, (cls, itf) -> symbolCls.equals(cls), includeInterfaces);
		} else {
			if("java/lang/Object".equals(requirementCls)) {
				return true; //Any non-primitive class is always assignable to Object, no need to traverse hierarchy
			}
			return this.classRelationResolver.traverseHierarchy(symbolCls, (cls, itf) -> requirementCls.equals(cls), includeInterfaces);
		}
	}

	/**
	 * Returns whether the specified symbol suffices the specified requirement
	 * @param requirement The symbol that determines the requirement
	 * @param symbol The symbol that is to be checked whether it suffices the requirement
	 * @return
	 * @throws TypeVariableResolvingException Thrown if a type variable cannot be resolved
	 */
	public boolean check(TypeSymbol requirement, TypeSymbol symbol) throws TypeVariableResolvingException {
		return this.check(requirement, symbol,
				new HashSet<>(), new HashSet<>(),
				new HashSet<>(), new HashSet<>(),
				true, '/', true);
	}

	private boolean check(TypeSymbol requirement, TypeSymbol symbol, 
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			boolean interfaces,
			char wildcard,
			boolean cycleFree) throws TypeVariableResolvingException {

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
			FormalTypeParameterSymbol resolvedRequirementParameter = this.requirementParameters.get(requirement.getName());
			if(resolvedRequirementParameter == null) {
				throw new TypeVariableResolvingException(String.format("Requirement type variable %s could not be resolved", requirement.getName()), requirement.getAsVariable(), true);
			}
			FormalTypeParameterSymbol resolvedSymbolParameter = this.symbolParameters.get(symbol.getName());
			if(resolvedSymbolParameter == null) {
				throw new TypeVariableResolvingException(String.format("Symbol type variable %s could not be resolved", symbol.getName()), symbol.getAsVariable(), false);
			}
			return this.check(resolvedRequirementParameter, resolvedSymbolParameter,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					wildcard, !isCurrentCycle && cycleFree);
		} else if(symbol.isVariable() && !requirement.isVariable()) {
			FormalTypeParameterSymbol resolvedSymbolParameter = this.symbolParameters.get(symbol.getName());
			if(resolvedSymbolParameter == null) {
				throw new TypeVariableResolvingException(String.format("Symbol type variable %s could not be resolved", symbol.getName()), symbol.getAsVariable(), false);
			}
			return this.check(requirement.getAsClass(), resolvedSymbolParameter,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					wildcard, !isCurrentCycle && cycleFree);
		} else if(requirement.isVariable()) {
			FormalTypeParameterSymbol resolvedRequirementParameter = this.requirementParameters.get(requirement.getName());
			if(resolvedRequirementParameter == null) {
				throw new TypeVariableResolvingException(String.format("Requirement type variable %s could not be resolved", requirement.getName()), requirement.getAsVariable(), true);
			}
			return this.check(resolvedRequirementParameter, symbol.getAsClass(),
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					wildcard, !isCurrentCycle && cycleFree);
		} else {
			switch(wildcard) {
			case '/':
			case SignatureVisitor.INSTANCEOF:
			case SignatureVisitor.EXTENDS:
				if(!this.isCompatible(requirement.getName(), symbol.getName(), interfaces, false)) {
					return false;
				}
				break;
			case SignatureVisitor.SUPER:
				//requirement class must be assignable to symbol, otherwise super is not satisfied
				if(!this.isCompatible(requirement.getName(), symbol.getName(), interfaces, true)) {
					return false;
				}
				break;
			}

			return this.checkArgs(requirement.getAsClass(), symbol.getAsClass(),
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					cycleFree);
		}
	}

	private boolean checkArgs(TypeClassSymbol requirement, TypeClassSymbol symbol, 
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			boolean cycleFree) throws TypeVariableResolvingException {
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

		boolean outer = resolvedRequirementParameters.isEmpty() || resolvedSymbolParameters.isEmpty();

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
				if(!this.check(requirementArg.getSymbol(), symbolArg.getSymbol(),
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, symbolArg.getWildcard(), cycleFree)) {
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
				if(!this.check(requirementArg.getSymbol(), symbolArg.getSymbol(),
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, symbolArg.getWildcard(), cycleFree)) {
					suffices = false;
					break args;
				}
				break;
			}
		}

		if(outer) {
			resolvedRequirementParameters.clear();
			resolvedSymbolParameters.clear();
			requirementCycleStart.clear();
			symbolCycleStart.clear();
		}

		return suffices;
	}

	private boolean check(FormalTypeParameterSymbol requirement, TypeClassSymbol symbol, 
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			char wildcard,
			boolean cycleFree) throws TypeVariableResolvingException {
		TypeSymbol requirementExtends = requirement.getExtends();
		if(requirementExtends != null) {
			if(!this.check(requirementExtends, symbol,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					false, wildcard, cycleFree)) {
				return false;
			}
		}

		for(TypeSymbol requirementImplements : requirement.getImplements()) {
			if(!this.check(requirementImplements, symbol,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					true, wildcard, cycleFree)) {
				return false;
			}
		}

		return true;
	}

	private boolean check(TypeClassSymbol requirement, FormalTypeParameterSymbol symbol, 
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			char wildcard,
			boolean cycleFree) throws TypeVariableResolvingException {
		TypeSymbol symbolExtends = symbol.getExtends();
		if(symbolExtends != null) {
			if(this.check(requirement, symbolExtends,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					false, wildcard, cycleFree)) {
				return true;
			}
		}

		for(TypeSymbol symbolImplements : symbol.getImplements()) {
			if(this.check(requirement, symbolImplements,
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					true, wildcard, cycleFree)) {
				return true;
			}
		}

		return false;
	}

	private boolean check(FormalTypeParameterSymbol requirement, FormalTypeParameterSymbol symbol, 
			Set<String> resolvedRequirementParameters, Set<String> resolvedSymbolParameters,
			Set<String> requirementCycleStart, Set<String> symbolCycleStart,
			char wildcard,
			boolean cycleFree) throws TypeVariableResolvingException {
		TypeSymbol requirementExtends = requirement.getExtends();
		if(requirementExtends != null) {
			TypeSymbol symbolExtends = symbol.getExtends();
			if(symbolExtends == null) {
				return false;
			}
			if(!this.check(requirementExtends, symbolExtends, 
					resolvedRequirementParameters, resolvedSymbolParameters, 
					requirementCycleStart, symbolCycleStart,
					false, wildcard, cycleFree)) {
				return false;
			}
		}

		for(TypeSymbol requirementImplements : requirement.getImplements()) {
			boolean suffices = false;
			for(TypeSymbol symbolImplements : symbol.getImplements()) {
				if(this.check(requirementImplements, symbolImplements, 
						resolvedRequirementParameters, resolvedSymbolParameters, 
						requirementCycleStart, symbolCycleStart,
						true, wildcard, cycleFree)) {
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
