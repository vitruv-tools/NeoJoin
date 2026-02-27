package tools.vitruv.neojoin.tgg.emsl_utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import static tools.vitruv.neojoin.tgg.emsl_utils.assertions.ModelNodeBlockAssert.assertThat;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.neo.emsl.eMSL.EMSLFactory;
import org.emoflon.neo.emsl.eMSL.EMSL_Spec;
import org.emoflon.neo.emsl.eMSL.Model;
import org.emoflon.neo.emsl.eMSL.ModelNodeBlock;
import org.emoflon.neo.emsl.eMSL.PrimitiveDouble;
import org.emoflon.neo.emsl.eMSL.PrimitiveInt;
import org.emoflon.neo.emsl.eMSL.PrimitiveString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.vitruv.neojoin.tgg.emsl_parser.EmslParser;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class EmslModelGeneratorTest extends AbstractEmslGeneratorTest {
    @TempDir Path tempDirectory;

    private static final String ECORE_MODEL_FILENAME = "SimpleCarModel.xmi";
    private static final String METAMODEL_OUTPUT_FILENAME = "metamodel-output.msl";
    private static final String MODEL_OUTPUT_FILENAME = "test-model-output.msl";

    private ResourceSet resourceSet;

    @BeforeEach
    void setup() {
        EmslParser.initialize();

        // Setup ResourceSet
        resourceSet = new ResourceSetImpl();
        resourceSet
                .getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());
        resourceSet
                .getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());

        // Load test model
        final URL modelUrl = getClass().getClassLoader().getResource(ECORE_MODEL_FILENAME);
        assertThat(modelUrl).isNotNull();

        Resource modelResource = resourceSet.getResource(URI.createURI(modelUrl.toString()), true);
        assertThat(modelResource).isNotNull();
        assertThat(modelResource.getContents()).isNotEmpty();

        // Generate the metamodel file
        final Path metamodelOutputPath = tempDirectory.resolve(METAMODEL_OUTPUT_FILENAME);
        EmslMetamodelGenerator.generateMetamodels(getMetamodelResourceSet(), metamodelOutputPath);
    }

    @Test
    void generateModels() throws IOException {
        // given
        final Path outputPath = tempDirectory.resolve(MODEL_OUTPUT_FILENAME);

        // when
        assertThatCode(() -> EmslModelGenerator.generateModels(resourceSet, outputPath))
                .doesNotThrowAnyException();

        // We need to add the metamodel to the model file to parse it correctly
        final String metamodelContent =
                Files.readString(tempDirectory.resolve(METAMODEL_OUTPUT_FILENAME));
        final String modelContent = Files.readString(outputPath);
        Files.writeString(outputPath, metamodelContent + modelContent);

        // then
        assertThat(outputPath).exists();

        // Parse the msl to EMF again
        final String absoluteOutputPath = outputPath.toAbsolutePath().toString();
        final List<EObject> parsedObjects = EmslParser.parse(absoluteOutputPath);
        assertThat(parsedObjects).isNotNull().isNotEmpty();

        final EObject rootObject = parsedObjects.getFirst();
        assertThat(rootObject).isInstanceOf(EMSL_Spec.class);

        final EMSL_Spec emslSpec = (EMSL_Spec) rootObject;
        assertThat(emslSpec.getEntities()).hasSize(2);

        final EObject firstModelEntity =
                emslSpec.getEntities().stream()
                        .filter(Model.class::isInstance)
                        .findFirst()
                        .orElseThrow();
        assertThat(firstModelEntity).isInstanceOf(Model.class);

        final Model model = (Model) firstModelEntity;
        assertThat(model.getName()).isEqualTo("SimpleCarModel");

        assertThat(model.getNodeBlocks()).hasSize(7);

        // Wheels
        final ModelNodeBlock wheel1001 = findWheelWithId(model.getNodeBlocks(), 1001);
        assertThat(wheel1001).hasPropertyCount(3);
        assertThat(wheel1001).hasEqualsProperty("wheelId", getPrimitiveInt(1001));
        assertThat(wheel1001).hasEqualsProperty("pressure", getPrimitiveDouble(2.2));
        assertThat(wheel1001).hasEqualsProperty("diameter", getPrimitiveInt(16));
        assertThat(wheel1001).hasNoRelations();

        final ModelNodeBlock wheel1002 = findWheelWithId(model.getNodeBlocks(), 1002);
        assertThat(wheel1002).hasPropertyCount(3);
        assertThat(wheel1002).hasEqualsProperty("wheelId", getPrimitiveInt(1002));
        assertThat(wheel1002).hasEqualsProperty("pressure", getPrimitiveDouble(2.1));
        assertThat(wheel1002).hasEqualsProperty("diameter", getPrimitiveInt(10));
        assertThat(wheel1002).hasNoRelations();

        final ModelNodeBlock wheel1003 = findWheelWithId(model.getNodeBlocks(), 1003);
        assertThat(wheel1003).hasPropertyCount(3);
        assertThat(wheel1003).hasEqualsProperty("wheelId", getPrimitiveInt(1003));
        assertThat(wheel1003).hasEqualsProperty("pressure", getPrimitiveDouble(1.83));
        assertThat(wheel1003).hasEqualsProperty("diameter", getPrimitiveInt(15));
        assertThat(wheel1003).hasNoRelations();

        final ModelNodeBlock wheel1004 = findWheelWithId(model.getNodeBlocks(), 1004);
        assertThat(wheel1004).hasPropertyCount(3);
        assertThat(wheel1004).hasEqualsProperty("wheelId", getPrimitiveInt(1004));
        assertThat(wheel1004).hasEqualsProperty("pressure", getPrimitiveDouble(2.2));
        assertThat(wheel1004).hasEqualsProperty("diameter", getPrimitiveInt(16));
        assertThat(wheel1004).hasNoRelations();

        // Axes
        final ModelNodeBlock axis100 = findAxisWithId(model.getNodeBlocks(), 100);
        assertThat(axis100).hasPropertyCount(2);
        assertThat(axis100).hasEqualsProperty("axisId", getPrimitiveInt(100));
        assertThat(axis100).hasEqualsProperty("position", getPrimitiveString("front"));
        assertThat(axis100)
                .hasRelationCount(2)
                .hasRelationTargets("wheels", List.of(wheel1001, wheel1002));

        final ModelNodeBlock axis101 = findAxisWithId(model.getNodeBlocks(), 101);
        assertThat(axis101).hasPropertyCount(2);
        assertThat(axis101).hasEqualsProperty("axisId", getPrimitiveInt(101));
        assertThat(axis101).hasEqualsProperty("position", getPrimitiveString("rear"));
        assertThat(axis101)
                .hasRelationCount(2)
                .hasRelationTargets("wheels", List.of(wheel1003, wheel1004));

        // Car
        final ModelNodeBlock car = findCarWithId(model.getNodeBlocks(), 1);
        assertThat(car).hasPropertyCount(3);
        assertThat(car).hasEqualsProperty("carId", getPrimitiveInt(1));
        assertThat(car).hasEqualsProperty("manufacturer", getPrimitiveString("Toyota"));
        assertThat(car).hasEqualsProperty("modelName", getPrimitiveString("Corolla"));
        assertThat(car).hasRelationCount(2).hasRelationTargets("axes", List.of(axis100, axis101));
    }

    private PrimitiveInt getPrimitiveInt(int value) {
        final PrimitiveInt primitive = EMSLFactory.eINSTANCE.createPrimitiveInt();
        primitive.setLiteral(value);
        return primitive;
    }

    private PrimitiveDouble getPrimitiveDouble(double value) {
        final PrimitiveDouble primitive = EMSLFactory.eINSTANCE.createPrimitiveDouble();
        primitive.setLiteral(value);
        return primitive;
    }

    private PrimitiveString getPrimitiveString(String value) {
        final PrimitiveString primitive = EMSLFactory.eINSTANCE.createPrimitiveString();
        primitive.setLiteral(value);
        return primitive;
    }

    private ModelNodeBlock findWheelWithId(List<ModelNodeBlock> nodes, int wheelId) {
        return nodes.stream()
                .filter(node -> node.getType().getName().equals("Wheel"))
                .filter(node -> hasWheelId(node, wheelId))
                .findFirst()
                .orElseThrow();
    }

    private boolean hasWheelId(ModelNodeBlock node, int wheelId) {
        return node.getProperties().stream()
                .anyMatch(
                        property ->
                                property.getType().getName().equals("wheelId")
                                        && property.getValue() instanceof PrimitiveInt intValue
                                        && intValue.getLiteral() == wheelId);
    }

    private ModelNodeBlock findAxisWithId(List<ModelNodeBlock> nodes, int axisId) {
        return nodes.stream()
                .filter(node -> node.getType().getName().equals("Axis"))
                .filter(node -> hasAxisId(node, axisId))
                .findFirst()
                .orElseThrow();
    }

    private boolean hasAxisId(ModelNodeBlock node, int axisId) {
        return node.getProperties().stream()
                .anyMatch(
                        property ->
                                property.getType().getName().equals("axisId")
                                        && property.getValue() instanceof PrimitiveInt intValue
                                        && intValue.getLiteral() == axisId);
    }

    private ModelNodeBlock findCarWithId(List<ModelNodeBlock> nodes, int carId) {
        return nodes.stream()
                .filter(node -> node.getType().getName().equals("Car"))
                .filter(node -> hasCarId(node, carId))
                .findFirst()
                .orElseThrow();
    }

    private boolean hasCarId(ModelNodeBlock node, int carId) {
        return node.getProperties().stream()
                .anyMatch(
                        property ->
                                property.getType().getName().equals("carId")
                                        && property.getValue() instanceof PrimitiveInt intValue
                                        && intValue.getLiteral() == carId);
    }
}
