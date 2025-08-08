package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EDataType;

import java.util.List;

/**
 * Represents a NeoJoin query file.
 *
 * @param export    info for the generated target meta-model
 * @param imports   imported packages
 * @param dataTypes data types from the source models that are used in the target model (does not include Ecore data types)
 * @param classes   all classes (incl. root) of the target model
 * @param root      root class (must also be contained in {@code classes})
 * @implNote The target model will not reference the given data types directly but a copy of them.
 */
public record AQR(
    AQRExport export,
    List<AQRImport> imports,
    List<EDataType> dataTypes,
    List<AQRTargetClass> classes,
    AQRTargetClass root
) {}
