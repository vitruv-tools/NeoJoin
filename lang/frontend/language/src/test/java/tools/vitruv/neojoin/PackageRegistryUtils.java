package tools.vitruv.neojoin;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.neojoin.utils.EMFUtils;

import static tools.vitruv.neojoin.utils.Assertions.check;

public interface PackageRegistryUtils extends HasPackageRegistry {

    default EPackage lookup(String packageName) {
        var pack = EMFUtils.collectAvailablePackages(getPackageRegistry()).stream()
            .filter(r -> r.getName().equals(packageName))
            .toList();
        check(pack.size() == 1);
        return pack.getFirst();
    }

    default EClassifier lookup(String packageName, String classifierName) {
        var classifier = lookup(packageName).getEClassifier(classifierName);
        check(classifier != null);
        return classifier;
    }

    default EStructuralFeature lookup(String packageName, String classifierName, String featureName) {
        var classifier = lookup(packageName, classifierName);
        check(classifier instanceof EClass);
        var feature = ((EClass) classifier).getEStructuralFeature(featureName);
        check(feature != null);
        return feature;
    }

}
