package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMethodMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A latent method matcher that identifies methods to instrument when redefining or rebasing a type.
 */
public class InlineInstrumentationMatcher implements LatentMethodMatcher {

    /**
     * A method matcher that matches any ignored method.
     */
    private final ElementMatcher<? super MethodDescription> ignoredMethods;

    /**
     * A method matcher that matches any predefined method.
     */
    private final ElementMatcher<? super MethodDescription> predefinedMethodSignatures;

    /**
     * Creates a matcher where only overridable or declared methods are matched unless those are ignored. Methods that
     * are declared by the target type are only matched if they are not ignored. Declared methods that are not found on the
     * target type are always matched.
     *
     * @param ignoredMethods A method matcher that matches any ignored method.
     * @param targetType     The target type of the instrumentation before adding any user methods.
     * @return A latent method matcher that identifies any method to instrument for a rebasement or redefinition.
     */
    protected static LatentMethodMatcher of(ElementMatcher<? super MethodDescription> ignoredMethods, TypeDescription targetType) {
        ElementMatcher.Junction<MethodDescription> predefinedMethodSignatures = none();
        for (MethodDescription methodDescription : targetType.getDeclaredMethods()) {
            ElementMatcher.Junction<MethodDescription> signature = methodDescription.isConstructor()
                    ? isConstructor()
                    : ElementMatchers.<MethodDescription>named(methodDescription.getName());
            signature = signature.and(returns(methodDescription.getReturnType()));
            signature = signature.and(takesArguments(methodDescription.getParameters().asTypeList()));
            predefinedMethodSignatures = predefinedMethodSignatures.or(signature);
        }
        return new InlineInstrumentationMatcher(ignoredMethods, predefinedMethodSignatures);
    }

    /**
     * Creates a new inline instrumentation matcher.
     *
     * @param ignoredMethods             A method matcher that matches any ignored method.
     * @param predefinedMethodSignatures A method matcher that matches any predefined method.
     */
    protected InlineInstrumentationMatcher(ElementMatcher<? super MethodDescription> ignoredMethods,
                                           ElementMatcher<? super MethodDescription> predefinedMethodSignatures) {
        this.ignoredMethods = ignoredMethods;
        this.predefinedMethodSignatures = predefinedMethodSignatures;
    }

    @Override
    public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
        return not(ignoredMethods).and(isOverridable().or(isDeclaredBy(instrumentedType)))
                .or(isDeclaredBy(instrumentedType).and(not(predefinedMethodSignatures)));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && ignoredMethods.equals(((InlineInstrumentationMatcher) other).ignoredMethods)
                && predefinedMethodSignatures.equals(((InlineInstrumentationMatcher) other).predefinedMethodSignatures);
    }

    @Override
    public int hashCode() {
        return 31 * ignoredMethods.hashCode() + predefinedMethodSignatures.hashCode();
    }

    @Override
    public String toString() {
        return "InlineInstrumentationMatcher{" +
                "ignoredMethods=" + ignoredMethods +
                ", predefinedMethodSignatures=" + predefinedMethodSignatures +
                '}';
    }
}