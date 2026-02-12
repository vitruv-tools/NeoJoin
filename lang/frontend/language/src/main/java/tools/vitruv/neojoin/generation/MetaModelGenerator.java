package tools.vitruv.neojoin.generation;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRFeature;
import tools.vitruv.neojoin.aqr.AQRFrom;
import tools.vitruv.neojoin.aqr.AQRTargetClass;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * Generates an {@link EPackage} from a given {@link AQR}.
 *
 * @see #generate()
 */
public class MetaModelGenerator {

    private static final EcoreFactory Ecore = EcoreFactory.eINSTANCE;

    private final AQR aqr;

    private final ModelInfo.Trace trace = ModelInfo.Trace.create();

    private final Map<EDataType, EDataType> dataTypeSourceMap = new HashMap<>();

    public MetaModelGenerator(AQR input) {
        this.aqr = input;

        // ecore data types are not copied and thereby mapped to themselves
        EMFUtils.getAllEDataTypes(EcorePackage.eINSTANCE).forEach(dataType -> {
            dataTypeSourceMap.put(dataType, dataType);
        });
    }

    /**
     * Generates an {@link EPackage} from the given {@link AQR}. The generated package may contain problems, see {@link ModelInfo#diagnostic()}.
     *
     * @implNote Generation is performed in a 2 phase process: First all classes are generated and afterward the classes
     * are populated with features. This is required because the type of a reference can be another class
     * that has not been generated yet.
     */
    public ModelInfo generate() {
        check(trace.aqrToTarget().isEmpty(), "meta-model already generated");

        var pack = createPackage();

        for (var dataType : aqr.dataTypes()) {
            var copy = EcoreUtil.copy(dataType);
            dataTypeSourceMap.put(dataType, copy);
            pack.getEClassifiers().add(copy);
            if (dataType instanceof EEnum) {
                trace.targetToSourceEnums().put((EEnum) copy, (EEnum) dataType);
            }
        }

        // phase 1: create classes
        aqr.classes().stream()
            .map(this::createClass)
            .forEach(pack.getEClassifiers()::add);

        // phase 2: populate classes
        aqr.classes().forEach(this::populateClass);

        // sanity check: validate generated meta-model
        var diagnostic = Diagnostician.INSTANCE.validate(pack);

        return new ModelInfo(pack, diagnostic, trace);
    }

    protected EPackage createPackage() {
        var pack = Ecore.createEPackage();
        pack.setName(aqr.export().name());
        pack.setNsPrefix(aqr.export().name());
        pack.setNsURI(aqr.export().uri().toString());
        return pack;
    }

    protected EClass createClass(AQRTargetClass targetClass) {
        EClass target = Ecore.createEClass();
        target.setName(targetClass.name());

        trace.targetToAqr().put(target, targetClass);
        trace.aqrToTarget().put(targetClass, target);
        if (targetClass.source() != null) {
            trace.aqrToSource().put(targetClass, targetClass.source().allFroms().map(AQRFrom::clazz).toList());
        }

        return target;
    }

    private void populateClass(AQRTargetClass targetClass) {
        var target = Objects.requireNonNull(trace.aqrToTarget().get(targetClass));
        var features = targetClass.features().stream().map(this::createFeature).toList();
        target.getEStructuralFeatures().addAll(features);
    }

    protected EStructuralFeature createFeature(AQRFeature feature) {
        EStructuralFeature eFeature;
        if (feature instanceof AQRFeature.Attribute attr) {
            eFeature = createAttribute(attr);
        } else if (feature instanceof AQRFeature.Reference ref) {
            eFeature = createReference(ref);
        } else {
            throw new IllegalStateException(); // sealed interface
        }
        eFeature.setName(feature.name());
        setFeatureOptions(eFeature, feature.options());
        return eFeature;
    }

    protected EAttribute createAttribute(AQRFeature.Attribute attr) {
        var attribute = Ecore.createEAttribute();
        var mappedType = dataTypeSourceMap.get(attr.type());
        check(mappedType != null);
        attribute.setEType(mappedType);
        return attribute;
    }

    protected EReference createReference(AQRFeature.Reference ref) {
        var reference = Ecore.createEReference();
        var targetClass = Objects.requireNonNull(trace.aqrToTarget().get(ref.type()));
        reference.setEType(targetClass);
        return reference;
    }

    protected void setFeatureOptions(EStructuralFeature feature, AQRFeature.Options options) {
        feature.setLowerBound(options.lowerBound());
        feature.setUpperBound(options.upperBound());
        feature.setOrdered(options.isOrdered());
        feature.setUnique(options.isUnique());
        feature.setChangeable(options.isChangeable());
        feature.setTransient(options.isTransient());
        feature.setVolatile(options.isVolatile());
        feature.setUnsettable(options.isUnsettable());
        feature.setDerived(options.isDerived());

        if (feature instanceof EAttribute attribute) {
            attribute.setID(options.isID());
        } else if (feature instanceof EReference reference) {
            reference.setContainment(options.isContainment());
        }
    }

}
