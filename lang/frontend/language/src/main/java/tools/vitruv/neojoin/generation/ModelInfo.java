package tools.vitruv.neojoin.generation;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRTargetClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ModelInfo(
	EPackage pack,
	Diagnostic diagnostic,
	Trace trace
) {

	/**
	 * Contains different mappings between source and target meta-models.
	 *
	 * @param aqrToSource         AQR.TargetClass -> List of referenced source classes
	 * @param targetToAqr         Target class -> AQR.TargetClass that generated it
	 * @param aqrToTarget         AQR.TargetClass -> target class generated from it
	 * @param targetToSourceEnums target enum -> source enum that it was copied from
	 */
	public record Trace(
		Map<AQRTargetClass, List<EClass>> aqrToSource,
		Map<EClass, AQRTargetClass> targetToAqr,
		Map<AQRTargetClass, EClass> aqrToTarget,
		Map<EEnum, EEnum> targetToSourceEnums
	) {

		public static Trace create() {
			return new Trace(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
		}

		public @Nullable AQRTargetClass getAQRForTargetClass(EClass target) {
			return targetToAqr.get(target);
		}

		public @Nullable List<EClass> getSourceClassesForTargetClass(EClass target) {
			var query = getAQRForTargetClass(target);
			if (query == null) {
				return null;
			} else {
				return aqrToSource.get(query);
			}
		}

	}

}
