package tools.vitruv.neojoin.ide.visualization;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRImport;
import tools.vitruv.neojoin.generation.ModelInfo;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Pair;
import tools.vitruv.neojoin.utils.Utils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static tools.vitruv.neojoin.ide.visualization.PlantUMLBuilder.Empty;
import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * Generates a PlantUML class diagram that corresponds to a given query. The generated diagram contains the
 * source meta-models, the generated target meta-model and arrows indicating which source classes correspond
 * to which target classes. Visualization supports 3 modes:
 * {@link Mode.Full Full}, {@link Mode.Referenced Referenced}, {@link Mode.Selected Selected}
 */
public class VisualizationGenerator {

    public static final String ArrowSourceClass = ".down.>";

    public sealed interface Mode {

        /**
         * Show all classes of the target meta-model and all classes of the source meta-models.
         */
        record Full() implements Mode {}

        /**
         * Show all classes of the target meta-model and the corresponding classes of the source meta-model
         * which are used to generate these target classes, i.e. are directly copied over or are
         * referenced in the target class' query.
         */
        record Referenced() implements Mode {}

        /**
         * Show the given selected classes of the target meta-model and corresponding classes of the source
         * meta-models (see {@link Mode.Referenced Referenced}).
         */
        record Selected(List<EClass> selectedClasses) implements Mode {}

    }

    private final AQR aqr;
    private final ModelInfo targetMetaModel;
    private final Mode mode;
    private final boolean orthogonalArrows;

    private final PlantUMLBuilder out = new PlantUMLBuilder();

    public VisualizationGenerator(AQR aqr, ModelInfo targetMetaModel, Mode mode, boolean orthogonalArrows) {
        this.aqr = aqr;
        this.targetMetaModel = targetMetaModel;
        this.mode = mode;
        this.orthogonalArrows = orthogonalArrows;
    }

    private @Nullable AQRImport getImport(EPackage pack) {
        for (var imp : aqr.imports()) {
            if (imp.pack() == pack) {
                return imp;
            }
        }
        return null;
    }

    private String getQualifiedName(EPackage pack) {
        if (pack == targetMetaModel.pack()) {
            return pack.getName();
        } else {
            var rootPack = EMFUtils.getRootPackage(pack);
            var imp = getImport(rootPack);
            check(
                imp != null,
                () -> "No import for package: %s (%s)".formatted(rootPack.getName(), rootPack.getNsURI())
            );

            return Utils.indexed(EMFUtils.getFullyQualifiedNameAsStream(pack))
                .map(part -> {
                    //noinspection DataFlowIssue - false positive
                    if (part.right() == 0) {
                        // first part is the root package, use import alias
                        return imp.alias();
                    } else {
                        return part.left();
                    }
                })
                .collect(Collectors.joining("."));
        }
    }

    private String getQualifiedName(EClassifier classifier) {
        if (classifier.getEPackage() == EcorePackage.eINSTANCE) {
            return classifier.getName();
        } else {
            var packName = getQualifiedName(classifier.getEPackage());
            return packName + "." + classifier.getName();
        }
    }

    public String generate() {
        check(out.isEmpty(), "Visualization already generated");

        out.document(() -> {
            header();
            packages(); // generate empty package stubs

            // Classifiers are sorted in the following sections to ensure a deterministic output, because otherwise
            // elements can jump around for example when reformatting the source file or adding spaces.

            if (mode instanceof Mode.Full) {
                // show all source classes
                aqr.imports().forEach(imp -> {
                    EMFUtils.getAllEClassifiers(imp.pack())
                        .sorted(Comparator.comparing(EClassifier::getName))
                        .forEach(classifier -> {
                            if (classifier instanceof EClass clazz) {
                                // class might have already been generated as super class of another class
                                clazzIfNew(clazz);
                            } else if (classifier instanceof EEnum eEnum) {
                                // enum might have already been generated from a class attribute
                                enumerationIfNew(eEnum);
                            }
                        });
                });
            }

            if (mode instanceof Mode.Selected(var selectedClasses)) {
                // show selected target classes
                selectedClasses.stream()
                    .sorted(Comparator.comparing(EClassifier::getName))
                    .forEach(this::targetClazz);
            } else {
                // show all target classes
                targetMetaModel.pack().getEClassifiers().stream()
                    .sorted(Comparator.comparing(EClassifier::getName))
                    .forEach(classifier -> {
                        if (classifier instanceof EClass clazz) {
                            targetClazz(clazz);
                        } else if (classifier instanceof EEnum eEnum) {
                            // enum might have already been generated from a class attribute
                            enumerationIfNew(eEnum);
                        }
                    });
            }
        });

        return out.build();
    }

    private void header() {
        out.appendln("""
            hide circle
            hide empty members
            !pragma useIntermediatePackages false
            <style>
                element {
                    header {
                        FontStyle: bold
                    }
                }
            </style>
            """.stripIndent().strip());
        if (orthogonalArrows) {
            out.appendln("skinparam linetype ortho");
        }
    }

    /**
     * Create an empty stub for each package.
     */
    private void packages() {
        // wrap all source packages within `together {}` to group them together
        out.block(
            "together", () -> {
                for (var imp : aqr.imports()) {
                    out.pack(imp.alias(), Empty);
                }
            }
        );

        var targetName = aqr.export().name();
        out.pack("\"Target: %s\" as %s".formatted(targetName, targetName), Empty);
    }

    private void targetClazz(EClass target) {
        clazz(target);

        // show arrow from source to target classes
        var sourceClasses = targetMetaModel.trace().getSourceClassesForTargetClass(target);
        if (sourceClasses != null) {
            sourceClasses.forEach(source -> {
                clazzIfNew(source);
                out.arrow(
                    getQualifiedName(source),
                    ArrowSourceClass,
                    getQualifiedName(target)
                );
            });
        }
    }

    private final HashSet<EClass> seenClazzes = new HashSet<>();

    private void clazzIfNew(EClass clazz) {
        if (!seenClazzes.contains(clazz)) {
            clazz(clazz);
        }
    }

    private void clazz(EClass clazz) {
        var isNew = seenClazzes.add(clazz);
        check(isNew);

        var qualifiedName = getQualifiedName(clazz);

        // class with attributes
        out.clazz(
            qualifiedName, () -> {
                clazz.getEAttributes().forEach(attr -> {
                    var type = getQualifiedName(attr.getEType());
                    out.attribute(attr.getName(), type);
                });
            }
        );

        // references
        clazz.getEReferences().forEach(ref -> {
            out.reference(
                qualifiedName,
                ref.isContainment() ? PlantUMLBuilder.ReferenceContainment : PlantUMLBuilder.ReferenceNormal,
                getQualifiedName(ref.getEType()),
                ref.getName(),
                new Pair<>(ref.getLowerBound(), ref.getUpperBound())
            );
        });

        // super types
        for (var superType : clazz.getESuperTypes()) {
            clazzIfNew(superType);
            var superName = getQualifiedName(superType);
            out.arrow(qualifiedName, PlantUMLBuilder.ReferenceInheritance, superName);
        }

        // referenced enumerations
        clazz.getEAttributes().forEach(attr -> {
            if (attr.getEType() instanceof EEnum eEnum) {
                enumerationIfNew(eEnum);
            }
        });
    }

    private final HashSet<EEnum> seenEnums = new HashSet<>();

    private void enumerationIfNew(EEnum eEnum) {
        if (!seenEnums.contains(eEnum)) {
            enumeration(eEnum);
        }
    }

    private void enumeration(EEnum eEnum) {
        var isNew = seenEnums.add(eEnum);
        check(isNew);

        var qualifiedName = getQualifiedName(eEnum);

        out.enumeration(
            qualifiedName, () -> {
                eEnum.getELiterals().forEach(literal -> {
                    out.literal(literal.getName());
                });
            }
        );

        var sourceEnum = targetMetaModel.trace().targetToSourceEnums().get(eEnum);
        if (sourceEnum != null) {
            enumerationIfNew(sourceEnum);
            var sourceName = getQualifiedName(sourceEnum);
            out.arrow(
                sourceName,
                ArrowSourceClass,
                qualifiedName
            );
        }
    }

}
