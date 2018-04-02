package pr0x79.signature;

import java.util.Map;

import org.objectweb.asm.Type;

import pr0x79.Accessors;
import pr0x79.BytecodeInstrumentation;
import pr0x79.ClassAccessorData;
import pr0x79.ClassHierarchy;
import pr0x79.ClassLocators;
import pr0x79.signature.SignatureParser.FormalTypeParameterSymbol;
import pr0x79.signature.SignatureParser.TypeSymbol;

public class AccessorAwareSignatureChecker extends SignatureChecker {
	private final Accessors accessors;
	private final ClassLocators locators;
	private final boolean bidirectional;

	public AccessorAwareSignatureChecker(ClassHierarchy hierarchy, ClassLoader loader, Accessors accessors, ClassLocators locators, boolean bidirectional) {
		super(hierarchy, loader);
		this.accessors = accessors;
		this.locators = locators;
		this.bidirectional = bidirectional;
	}

	@Override
	public boolean check(TypeSymbol requirement, TypeSymbol symbol,
			Map<String, FormalTypeParameterSymbol> requirementParameters, 
			Map<String, FormalTypeParameterSymbol> symbolParameters,
			boolean checkArgs) throws TypeVariableResolvingException {
		if(this.bidirectional && !super.check(symbol, requirement, symbolParameters, requirementParameters, checkArgs)) {
			return false;
		}
		return super.check(requirement, symbol, requirementParameters, symbolParameters, checkArgs);
	}

	@Override
	protected boolean isCompatible(String requirementCls, String symbolCls, boolean includeInterfaces, boolean isSuper, boolean isKnownDowncast) {
		//Downcasts can be allowed if the symbolCls is an accessor of requirementCls or vice versa

		if(this.bidirectional) {
			//Both directions must be compatible -> no hierarchy traversal necessary, compare directly

			if(requirementCls.equals(symbolCls)) {
				return true;
			}

			ClassAccessorData requirementAccessor = this.accessors.getAccessorByClassName(Type.getObjectType(requirementCls).getClassName());
			if(requirementAccessor != null && BytecodeInstrumentation.isIdentifiedClass(requirementAccessor, symbolCls, flags -> this.locators.getClass(this.loader, symbolCls, flags))) {
				return true;
			}

			ClassAccessorData symbolAccessor = this.accessors.getAccessorByClassName(Type.getObjectType(symbolCls).getClassName());
			if(symbolAccessor != null && BytecodeInstrumentation.isIdentifiedClass(symbolAccessor, requirementCls, flags -> this.locators.getClass(this.loader, requirementCls, flags))) {
				return true;
			}

			return false;
		} else {
			if(isSuper) {
				if("java/lang/Object".equals(symbolCls)) {
					return true; //Object will always be assignable to any <? super X>
				}

				ClassAccessorData symbolAccessor = this.accessors.getAccessorByClassName(Type.getObjectType(symbolCls).getClassName());

				return this.classRelationResolver.traverseHierarchy(requirementCls, (cls, itf, clsNode, clsFlags) -> {
					if(symbolAccessor != null) {
						if(BytecodeInstrumentation.isIdentifiedClass(symbolAccessor, cls, flags -> {
							if(clsNode != null && flags == clsFlags) {
								return clsNode;
							}
							return this.locators.getClass(this.loader, cls, flags);
						})) {
							return true;
						}
					}

					ClassAccessorData requirementAccessor = this.accessors.getAccessorByClassName(Type.getObjectType(cls).getClassName());

					//Check if (sub-)class/interface of requirement is an accessor of the symbolCls
					if(requirementAccessor != null) {
						if(BytecodeInstrumentation.isIdentifiedClass(requirementAccessor, symbolCls, flags -> {
							if(clsNode != null && symbolCls.equals(clsNode.name) && flags == clsFlags) {
								return clsNode;
							}
							return this.locators.getClass(this.loader, symbolCls, flags);
						})) {
							return true;
						}
					}

					return symbolCls.equals(cls);
				}, includeInterfaces);
			} else {
				if("java/lang/Object".equals(requirementCls)) {
					return true; //Any non-primitive class is always assignable to Object, no need to traverse hierarchy
				}

				ClassAccessorData requirementAccessor = this.accessors.getAccessorByClassName(Type.getObjectType(requirementCls).getClassName());

				return this.classRelationResolver.traverseHierarchy(symbolCls, (cls, itf, clsNode, clsFlags) -> {
					ClassAccessorData symbolAccessor = this.accessors.getAccessorByClassName(Type.getObjectType(cls).getClassName());

					if(symbolAccessor != null) {
						if(BytecodeInstrumentation.isIdentifiedClass(symbolAccessor, requirementCls, flags -> {
							if(clsNode != null && requirementCls.equals(clsNode.name) && flags == clsFlags) {
								return clsNode;
							}
							return this.locators.getClass(this.loader, requirementCls, flags);
						})) {
							return true;
						}
					}

					//Check if accessor is an accessor of this (super-)class/interface
					if(requirementAccessor != null) {
						if(BytecodeInstrumentation.isIdentifiedClass(requirementAccessor, cls, flags -> {
							if(clsNode != null && flags == clsFlags) {
								return clsNode;
							}
							return this.locators.getClass(this.loader, cls, flags);
						})) {
							return true;
						}
					}

					return requirementCls.equals(cls);
				}, includeInterfaces);
			}
		}
	}
}
