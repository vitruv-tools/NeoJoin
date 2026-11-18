package tools.vitruv.neojoin.collector;

import org.eclipse.xtext.diagnostics.Severity;

import java.util.List;

/**
 * A problem found while collecting models from a model path. The message is the {@link Object#toString() string
 * representation}. Collectors skip the affected model and continue; an {@link Severity#ERROR error} means that the
 * remaining models are not a sound input (for example for a transformation), a {@link Severity#WARNING warning} that
 * they are.
 */
public sealed interface CollectorIssue {

    Severity severity();

    /**
     * A file on the model path cannot be loaded, or a path entry does not exist. The file is skipped: a query that
     * imports its meta-model reports the meta-model as unknown, a transformation that selects instances of its
     * meta-model reports the instance model as missing.
     *
     * @param location URI of the file or the path entry
     * @param reason   description of the problem
     */
    record LoadFailure(String location, String reason) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Cannot load '%s': %s".formatted(location, reason);
        }
    }

    /**
     * Several files define meta-models with the same namespace URI; which one is meant is unknown, so none is used.
     */
    record DuplicateNamespaceUri(String packageUri, List<String> files) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Ignoring the meta-models with the namespace URI '%s': several files define it ('%s')."
                .formatted(packageUri, String.join("', '", files));
        }
    }

    /**
     * The namespace URI of a collected package is already in the global package registry (for example a copy of Ecore
     * from EMF's jar). The global package is used instead: the instance models are loaded with it, and it must not be
     * replaced for the namespace URI references of the other meta-models.
     */
    record GlobalPackageConflict(String packageUri, String file) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return ("Ignoring the meta-model '%s' in '%s': a package with this namespace URI is registered globally "
                + "and used instead.").formatted(packageUri, file);
        }
    }

    record ReferenceOutsideModelPath(String packageUri, String referencedUri) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Ignoring the meta-model '%s': it references '%s', which is not on the meta-model path."
                .formatted(packageUri, referencedUri);
        }
    }

    record ReferenceToIgnoredMetaModel(String packageUri, String referencedPackageUri) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Ignoring the meta-model '%s': it references the ignored meta-model '%s'."
                .formatted(packageUri, referencedPackageUri);
        }
    }

    record DataTypeWithoutInstanceClass(String packageUri, String dataTypeUri) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Ignoring the meta-model '%s': the data type '%s' has no instance class."
                .formatted(packageUri, dataTypeUri);
        }
    }

    record FeatureWithoutType(String packageUri, String featureName) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Ignoring the meta-model '%s': the feature '%s' has no type.".formatted(packageUri, featureName);
        }
    }

    record MetaModelOutsideModelPath(String instanceModelUri, String packageUri) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.WARNING;
        }

        @Override
        public String toString() {
            return "Ignoring the instance model '%s': it uses the meta-model '%s', which is not on the meta-model path."
                .formatted(instanceModelUri, packageUri);
        }
    }

    /**
     * The transformation expects at most one instance model per meta-model. Files that cannot be loaded count too if
     * their root element names the meta-model, because one of them may be the intended instance model.
     */
    record MultipleInstanceModels(String packageUri, String firstUri, String secondUri) implements CollectorIssue {

        @Override
        public Severity severity() {
            return Severity.ERROR;
        }

        @Override
        public String toString() {
            return "Found several instance models of the meta-model '%s': '%s' and '%s'."
                .formatted(packageUri, firstUri, secondUri);
        }
    }
}
