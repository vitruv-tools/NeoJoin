package tools.vitruv.neojoin.tgg.emsl_utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import static tools.vitruv.neojoin.tgg.emsl_utils.assertions.MetamodelNodeBlockAssert.assertThat;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.emoflon.neo.emsl.eMSL.BuiltInDataTypes;
import org.emoflon.neo.emsl.eMSL.EMSL_Spec;
import org.emoflon.neo.emsl.eMSL.Metamodel;
import org.emoflon.neo.emsl.eMSL.MetamodelNodeBlock;
import org.emoflon.neo.emsl.eMSL.RelationKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.vitruv.neojoin.emsl_parser.EmslParser;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

class EmslMetamodelGeneratorTest {
    @TempDir Path tempDirectory;

    private static final String ECORE_METAMODEL_FILENAME = "SimpleCarMetamodel.ecore";

    private ResourceSet resourceSet;

    @BeforeEach
    void setup() {
        // Setup ResourceSet
        resourceSet = new ResourceSetImpl();
        resourceSet
                .getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());

        // Load test model
        final URL resourceUrl = getClass().getClassLoader().getResource(ECORE_METAMODEL_FILENAME);
        assertThat(resourceUrl).isNotNull();
        final URI ecoreResourceURI = URI.createURI(resourceUrl.toString());

        final Resource ecoreResource = resourceSet.getResource(ecoreResourceURI, true);
        assertThat(ecoreResource).isNotNull();
        assertThat(ecoreResource.getContents()).isNotEmpty();
    }

    @Test
    void generateMetamodels() {
        // given
        final Path outputPath = tempDirectory.resolve("test-metamodel-output.msl");

        // when
        assertThatCode(() -> EmslMetamodelGenerator.generateMetamodels(resourceSet, outputPath))
                .doesNotThrowAnyException();

        // then
        assertThat(outputPath).exists();

        // Parse the msl to EMF again
        final String absoluteOutputPath = outputPath.toAbsolutePath().toString();
        final List<EObject> parsedObjects = EmslParser.parse(absoluteOutputPath);
        assertThat(parsedObjects).isNotNull().isNotEmpty();

        final EObject rootObject = parsedObjects.getFirst();
        assertThat(rootObject).isInstanceOf(EMSL_Spec.class);

        final EMSL_Spec emslSpec = (EMSL_Spec) rootObject;
        assertThat(emslSpec.getEntities()).hasSize(1);

        final EObject firstEntity = emslSpec.getEntities().getFirst();
        assertThat(firstEntity).isInstanceOf(Metamodel.class);

        final Metamodel metamodel = (Metamodel) firstEntity;
        assertThat(metamodel.getName()).isEqualTo("SimpleCarMetamodel");

        assertThat(metamodel.getNodeBlocks()).hasSize(3);

        // Wheel
        final MetamodelNodeBlock wheelNodeBlock =
                metamodel.getNodeBlocks().stream()
                        .filter(node -> node.getName().equals("Wheel"))
                        .findFirst()
                        .orElseThrow();
        assertThat(wheelNodeBlock).hasName("Wheel").hasPropertyCount(3);
        assertThat(wheelNodeBlock).hasProperty("wheelId", BuiltInDataTypes.EINT);
        assertThat(wheelNodeBlock).hasProperty("pressure", BuiltInDataTypes.EDOUBLE);
        assertThat(wheelNodeBlock).hasProperty("diameter", BuiltInDataTypes.EINT);
        assertThat(wheelNodeBlock).hasNoRelations();

        // Axis
        final MetamodelNodeBlock axisNodeBlock =
                metamodel.getNodeBlocks().stream()
                        .filter(node -> node.getName().equals("Axis"))
                        .findFirst()
                        .orElseThrow();
        assertThat(axisNodeBlock).hasName("Axis").hasPropertyCount(2);
        assertThat(axisNodeBlock).hasProperty("axisId", BuiltInDataTypes.EINT);
        assertThat(axisNodeBlock).hasProperty("position", BuiltInDataTypes.ESTRING);

        assertThat(axisNodeBlock).hasRelationCount(1);
        assertThat(axisNodeBlock)
                .relation("wheels")
                .hasKind(RelationKind.AGGREGATION)
                .hasName("wheels")
                .hasLowerBound("0")
                .hasUpperBound("*")
                .hasTarget(wheelNodeBlock);

        // Car
        final MetamodelNodeBlock carNodeBlock =
                metamodel.getNodeBlocks().stream()
                        .filter(node -> node.getName().equals("Car"))
                        .findFirst()
                        .orElseThrow();
        assertThat(carNodeBlock).hasName("Car").hasPropertyCount(3);
        assertThat(carNodeBlock).hasProperty("carId", BuiltInDataTypes.EINT);
        assertThat(carNodeBlock).hasProperty("manufacturer", BuiltInDataTypes.ESTRING);
        assertThat(carNodeBlock).hasProperty("modelName", BuiltInDataTypes.ESTRING);

        assertThat(carNodeBlock).hasRelationCount(1);
        assertThat(carNodeBlock)
                .relation("axes")
                .hasKind(RelationKind.AGGREGATION)
                .hasName("axes")
                .hasLowerBound("0")
                .hasUpperBound("*")
                .hasTarget(axisNodeBlock);
    }
}
