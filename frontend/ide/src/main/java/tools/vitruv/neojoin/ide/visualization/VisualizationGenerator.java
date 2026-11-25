package tools.vitruv.neojoin.ide.visualization;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.common.ClassifierNames;
import tools.vitruv.neojoin.common.EMFUtils;
import tools.vitruv.neojoin.generation.ModelInfo;
import tools.vitruv.neojoin.model.AQR;
import tools.vitruv.neojoin.model.AQRImport;

import tools.vitruv.neojoin.model.AQRParameter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.common.Enumerated.enumerated;
import static tools.vitruv.neojoin.common.Assertions.check;
import static tools.vitruv.neojoin.ide.visualization.PlantUMLBuilder.Empty;

/**
 * Generates a PlantUML class diagram that corresponds to a given query. The generated diagram contains the
 * source meta-models, the generated target meta-model and arrows indicating which source classes correspond
 * to which target classes. Visualization supports 3 modes:
 * {@link Mode.Full Full}, {@link Mode.Referenced Referenced}, {@link Mode.Selected Selected}
 * <p>
 * Classes and enumerations of packages that are not imported (referenced by source classes) are shown as empty boxes
 * in a package with the stereotype {@code <<external>>}.
 */
public class VisualizationGenerator {

    // starts with a digit, so that it cannot be the alias of an import (an identifier), like the ids of external
    // packages
    private static final String TargetPackageId = "0target";
    private static final String ListTypeFormat = "EList<%s>";

    public sealed interface Mode {

        /**
         * Show all classes of the target meta-model and all classes of the source meta-models.
         */
        record Full() implements Mode {
        }

        /**
         * Show all classes of the target meta-model and the corresponding classes of the source meta-model
         * which are used to generate these target classes, i.e. are directly copied over or are
         * referenced in the target class' query.
         */
        record Referenced() implements Mode {
        }

        /**
         * Show the given selected classes of the target meta-model and corresponding classes of the source
         * meta-models (see {@link Mode.Referenced Referenced}).
         */
        record Selected(List<EClass> selectedClasses) implements Mode {
        }

    }

    private final AQR aqr;
    private final ModelInfo targetMetaModel;
    private final Mode mode;
    private final boolean orthogonalArrows;

    // header, packages and external packages; written after the body, because the body decides which external
    // packages are shown, and PlantUML needs the declaration of a package before the first line that uses it
    // (otherwise it creates the package with its id as title)
    private final PlantUMLBuilder document = new PlantUMLBuilder();
    private final PlantUMLBuilder out = new PlantUMLBuilder();

    // external packages are root packages that are not imported; they get a generated id because the dots of a name
    // separate packages, and an empty element for each class or enumeration that an arrow points to
    private final Map<EPackage, String> externalIds = new LinkedHashMap<>();
    private final Set<EClassifier> externalElements = new HashSet<>();

    // attribute types are text, written like in validation messages: qualified only if the simple name is ambiguous
    private final ClassifierNames attributeTypeNames;

    public VisualizationGenerator(AQR aqr, ModelInfo targetMetaModel, Mode mode, boolean orthogonalArrows) {
        this.aqr = aqr;
        this.targetMetaModel = targetMetaModel;
        this.mode = mode;
        this.orthogonalArrows = orthogonalArrows;
        this.attributeTypeNames = createAttributeTypeNames(aqr);
    }

    /**
     * Creates the names of attribute types. Their candidates are the data types of the imported packages and Ecore
     * and of the packages that these reference, like for data types in validation messages.
     */
    private static ClassifierNames createAttributeTypeNames(AQR aqr) {
        var aliasByRootPackage = aqr.imports().stream()
            .collect(Collectors.toMap(AQRImport::pack, AQRImport::alias, (first, second) -> first));
        var startPackages = Stream.concat(aliasByRootPackage.keySet().stream(), Stream.of(EcorePackage.eINSTANCE))
            .toList();
        return new ClassifierNames(aliasByRootPackage, startPackages);
    }

    private @Nullable AQRImport getImport(EPackage pack) {
        for (var imp : aqr.imports()) {
            if (imp.pack() == pack) {
                return imp;
            }
        }
        return null;
    }

    private boolean isExternal(EClassifier classifier) {
        var pack = classifier.getEPackage();
        return pack != targetMetaModel.pack()
            && getImport(EMFUtils.getRootPackage(pack)) == null;
    }

    /**
     * Returns the name of an attribute type: the simple name within the target package, otherwise as in validation
     * messages (see {@link ClassifierNames}).
     */
    private String getAttributeTypeName(EClassifier type) {
        if (type.getEPackage() == targetMetaModel.pack()) {
            return type.getName();
        }
        return attributeTypeNames.write(type).plain();
    }

    private String replaceRootName(EPackage pack, String rootName) {
        return EMFUtils.getFullyQualifiedNameAsStream(pack)
            .map(enumerated())
            .map(part -> part.index() == 0 ? rootName : part.value())
            .collect(Collectors.joining("."));
    }

    /**
     * Returns the name of the diagram element of the classifier, which is also the end of arrows: the id of its root
     * package (import alias, {@code 0target} or the id of an external package), its sub packages and its name.
     * {@link #showExternalElement(EClassifier)} has to be called first for an external classifier.
     */
    private String getElementName(EClassifier classifier) {
        var pack = classifier.getEPackage();
        if (pack == targetMetaModel.pack()) {
            return TargetPackageId + "." + classifier.getName();
        }

        var rootPack = EMFUtils.getRootPackage(pack);
        var imp = getImport(rootPack);
        check(imp != null || externalElements.contains(classifier), "External classifier has no element");
        // an external package gets its id together with its first element
        var rootId = imp != null ? imp.alias() : Objects.requireNonNull(externalIds.get(rootPack));
        return replaceRootName(pack, rootId) + "." + classifier.getName();
    }

    /**
     * Writes the empty element of a classifier of an external package when it is used for the first time, and gives its
     * package an id. Does nothing for other classifiers. The element has no attributes, references, super types or
     * literals, because the diagram only needs it as end of arrows.
     */
    private void showExternalElement(EClassifier classifier) {
        if (!isExternal(classifier) || !externalElements.add(classifier)) {
            return;
        }

        // the id starts with a digit, so that it cannot be the alias of an import (an identifier)
        externalIds.computeIfAbsent(
            EMFUtils.getRootPackage(classifier.getEPackage()),
            pack -> (externalIds.size() + 1) + "external"
        );

        if (classifier instanceof EClass clazz) {
            out.clazz(getElementName(classifier), clazz.isAbstract(), Empty);
        } else if (classifier instanceof EEnum) {
            out.enumeration(getElementName(classifier), Empty);
        }
    }

    public String generate() {
        check(out.isEmpty(), "Visualization already generated");

        if (mode instanceof Mode.Full) {
            showClasses(getAllSourceClasses(), false);
            showClasses(getAllTargetClasses(), true);
        } else if (mode instanceof Mode.Selected selected) {
            showClasses(selected.selectedClasses.stream(), true);
        } else if (mode instanceof Mode.Referenced) {
            showClasses(getAllTargetClasses(), true);
        }

        document.document(() -> {
            document.header(orthogonalArrows);
            packages(); // generate empty package stubs
            externalPackages();
            if (!aqr.parameters().isEmpty()) {
                showParameterLegend(aqr.parameters());
            }

            document.include(out);
        });

        return document.build();
    }

    /**
     * @param target whether the classes are target classes (with arrows from their source classes)
     */
    private void showClasses(Stream<? extends EClassifier> classifiers, boolean target) {
        classifiers
            // Classifiers are sorted in the following sections to ensure a deterministic output, because otherwise
            // elements can jump around for example when reformatting the source file or adding spaces.
            .sorted(Comparator.comparing(EClassifier::getName))
            .forEach(classifier -> {
                if (classifier instanceof EClass clazz) {
                    // class might have already been generated as super class of another class
                    if (target) {
                        targetClazzIfNew(clazz);
                    } else {
                        clazzIfNew(clazz);
                    }
                } else if (classifier instanceof EEnum eEnum) {
                    // enum might have already been generated from a class attribute
                    enumerationIfNew(eEnum);
                }
            });
    }

    /**
     * Create an empty stub for each package.
     */
    private void packages() {
        // wrap all source packages within `together {}` to group them together
        document.together(() -> aqr.imports().forEach(imp -> document.sourcePackage(imp.alias())));

        document.targetPackage("Target: " + aqr.export().name(), TargetPackageId);
    }

    /**
     * Create an empty stub for each external package that has an element, with its title.
     */
    private void externalPackages() {
        externalIds.forEach((rootPack, id) -> document.externalPackage(getExternalTitle(rootPack), id));
    }

    /**
     * The name of the package, or with its namespace URI if another shown root package has the same title.
     */
    private String getExternalTitle(EPackage rootPack) {
        var titles = Stream.concat(
            aqr.imports().stream().map(AQRImport::alias),
            externalIds.keySet().stream().map(EPackage::getName)
        );
        var isNameShared = titles.filter(rootPack.getName()::equals).count() > 1;

        return isNameShared ? "%s (%s)".formatted(rootPack.getName(), rootPack.getNsURI()) : rootPack.getName();
    }

    private void targetClazzIfNew(EClass target) {
        if (seenClazzes.add(target)) {
            targetClazz(target);
        }
    }

    private void targetClazz(EClass target) {
        clazz(target, true);

        // show arrow from source to target classes
        var sourceClasses = targetMetaModel.trace().getSourceClassesForTargetClass(target);
        if (sourceClasses != null) {
            sourceClasses.forEach(source -> {
                clazzIfNew(source);
                showExternalElement(source);
                out.sourceToTarget(getElementName(source), getElementName(target));
            });
        }
    }

    private final HashSet<EClass> seenClazzes = new HashSet<>();

    private void clazzIfNew(EClass clazz) {
        // external classes are only drawn as empty boxes where an arrow points to them
        if (!isExternal(clazz) && seenClazzes.add(clazz)) {
            clazz(clazz, false);
        }
    }

    private void clazz(EClass clazz, boolean target) {
        var elementName = getElementName(clazz);

        // class with attributes
        out.clazz(
            elementName, clazz.isAbstract(), () -> clazz.getEAttributes().forEach(attr -> {
                var type = getAttributeTypeName(attr.getEType());
                out.attribute(attr.getName(), type);
            })
        );

        // references
        clazz.getEReferences().forEach(ref -> {
            showExternalElement(ref.getEType());
            out.reference(
                elementName,
                ref.isContainment(),
                getElementName(ref.getEType()),
                ref.getName(),
                ref.getLowerBound(),
                ref.getUpperBound()
            );
        });

        // super types
        for (var superType : clazz.getESuperTypes()) {
            if (target) {
                targetClazzIfNew(superType);
            } else {
                clazzIfNew(superType);
            }
            showExternalElement(superType);
            out.inheritance(elementName, getElementName(superType));
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
        if (!isExternal(eEnum) && seenEnums.add(eEnum)) {
            enumeration(eEnum);
        }
    }

    private void enumeration(EEnum eEnum) {
        var elementName = getElementName(eEnum);

        out.enumeration(
            elementName, () -> eEnum.getELiterals().forEach(literal -> out.literal(literal.getName()))
        );

        var sourceEnum = targetMetaModel.trace().targetToSourceEnums().get(eEnum);
        if (sourceEnum != null) {
            enumerationIfNew(sourceEnum);
            showExternalElement(sourceEnum);
            out.sourceToTarget(getElementName(sourceEnum), elementName);
        }
    }

    private Stream<EClassifier> getAllSourceClasses() {
        return aqr.imports().stream().flatMap(imp -> EMFUtils.getAllEClassifiers(imp.pack()));
    }

    private Stream<EClassifier> getAllTargetClasses() {
        return targetMetaModel.pack().getEClassifiers().stream();
    }

    private void showParameterLegend(List<AQRParameter> parameters) {
        out.legend("Parameters", "left", () -> {
            for (var parameter : parameters) {
                out.attribute(parameter.alias(), formatParameterType(parameter));
            }
        });
    }

    private static String formatParameterType(AQRParameter parameter) {
        return parameter.isList()
            ? ListTypeFormat.formatted(parameter.type().getName())
            : parameter.type().getName();
    }
}
