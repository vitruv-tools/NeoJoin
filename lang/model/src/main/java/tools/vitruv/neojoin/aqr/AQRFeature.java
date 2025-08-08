package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.Nullable;

/**
 * A feature of a target class can be either an {@link Attribute} or a {@link Reference} and has a {@link #name() name},
 * {@link #kind() kind}, {@link #options() options} and a type.
 */
public sealed interface AQRFeature {

    /**
     * Represents how a feature is derived from the source models.
     *
     * @see Kind.Calculate
     * @see Kind.Copy
     * @see Kind.Generate
     */
    sealed interface Kind {

        @Nullable
        default XExpression expression() {
            return null;
        }

        @Nullable
        default EStructuralFeature source() {
            return null;
        }

        /**
         * The feature is calculated from the source model by the given expression.
         *
         * @param expression
         */
        record Calculate(XExpression expression) implements Kind {}

        /**
         * The feature is copied from the source model retaining all options of the given
         * {@link #source() source feature}.
         *
         * @see Kind.Copy.Explicit
         * @see Kind.Copy.Implicit
         */
        sealed interface Copy extends Kind {

            @Override
            EStructuralFeature source();

            /**
             * Explicit copy with an expression.
             *
             * @param source     source feature (may not be a feature of the containing target class)
             * @param expression expression to calculate the value
             */
            record Explicit(
                EStructuralFeature source,
                XExpression expression
            ) implements Copy {}

            /**
             * Implicit copy.
             *
             * @param source source feature (must be a feature of the containing target class)
             */
            record Implicit(EStructuralFeature source) implements Copy {}

        }

        /**
         * The feature is generated internally. Currently only used for automatic containment references in the
         * root class.
         */
        record Generate() implements Kind {}

    }

    /**
     * Options of the feature as defined by Ecore (see
     * <a href="https://download.eclipse.org/modeling/emf/emf/javadoc/2.10.0/org/eclipse/emf/ecore/package-summary.html">here</a> or
     * <a href="https://www.philmann-dark.de/EMFDocs/tutorial.html#:~:text=in%20Java%20itself.-,Property,-Description">here</a>).
     *
     * @param lowerBound
     * @param upperBound
     * @param isOrdered
     * @param isUnique
     * @param isChangeable
     * @param isTransient
     * @param isVolatile
     * @param isUnsettable
     * @param isDerived
     * @param isID          (only for attributes)
     * @param isContainment (only for references)
     */
    record Options(
        int lowerBound,
        int upperBound,
        boolean isOrdered,
        boolean isUnique,
        boolean isChangeable,
        boolean isTransient,
        boolean isVolatile,
        boolean isUnsettable,
        boolean isDerived,
        boolean isID,
        boolean isContainment
    ) {

        /**
         * @return whether this is a multi-valued feature, i.e. its upper bound is greater than 1 (incl. unbounded)
         */
        public boolean isMany() {
            return upperBound == ETypedElement.UNBOUNDED_MULTIPLICITY || upperBound > 1;
        }

        /**
         * @return whether the feature requires at least one value
         */
        public boolean isRequired() {
            return lowerBound > 0;
        }

    }

    String name();

    Kind kind();

    Options options();

    /**
     * Represents an attribute.
     *
     * @param name
     * @param type    data type of the attribute
     * @param kind
     * @param options
     */
    record Attribute(
        String name,
        EDataType type,
        Kind kind,
        Options options
    ) implements AQRFeature {

        @Override
        public String toString() {
            return "Attribute[name='%s', type=%s, kind=%s, options=%s]".formatted(name, type.getName(), kind, options);
        }

    }

    /**
     * Represents a reference.
     *
     * @param name
     * @param type    referenced target class
     * @param kind
     * @param options
     */
    record Reference(
        String name,
        AQRTargetClass type,
        Kind kind,
        Options options
    ) implements AQRFeature {

        @Override
        public String toString() {
            return "Reference[name='%s', type=%s, kind=%s, options=%s]".formatted(name, type.name(), kind, options);
        }

    }

}
