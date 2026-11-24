package tools.vitruv.neojoin.ide.integration;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.collector.PackageModelCollector;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.generation.ModelInfo;
import tools.vitruv.neojoin.ide.visualization.VisualizationGenerator;
import tools.vitruv.neojoin.ide.visualization.VisualizationGenerator.Mode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The diagram shows classifiers of packages that the query does not import as empty boxes in an {@code <<external>>}
 * package. Runs against the shaded jar like {@link NeoJoinIdeServerIT}, calls the generator directly because the jar
 * does not contain PlantUML.
 */
class VisualizationGeneratorIT {

    private static final String XMI_HEADER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
        """;

    private static final String ORDER_IMPORT = "import \"http://example.org/order\"";

    @TempDir
    Path directory;

    /**
     * {@code order} (imported by the queries) refers to a super class, references, an enumeration and a data type
     * of three packages that the queries do not import. Two of them have the name {@code supplier}.
     */
    @BeforeEach
    void writeMetaModels() throws IOException {
        writeMetaModel("order", """
            name="order" nsURI="http://example.org/order" nsPrefix="order">
              <eClassifiers xsi:type="ecore:EClass" name="Order" eSuperTypes="http://example.org/supplier/a#//Item">
                <eStructuralFeatures xsi:type="ecore:EReference" name="fromA"
                    eType="ecore:EClass http://example.org/supplier/a#//Food"/>
                <eStructuralFeatures xsi:type="ecore:EReference" name="part"
                    eType="ecore:EClass http://example.org/supplier/a#//sub/Part"/>
                <eStructuralFeatures xsi:type="ecore:EReference" name="fromB"
                    eType="ecore:EClass http://example.org/supplier/b#//Food"/>
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="unit"
                    eType="ecore:EEnum http://example.org/units#//Unit"/>
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="code"
                    eType="ecore:EDataType http://example.org/supplier/b#//Code"/>
              </eClassifiers>
            """);
        writeMetaModel("supplier-a", """
            name="supplier" nsURI="http://example.org/supplier/a" nsPrefix="suppliera">
              <eClassifiers xsi:type="ecore:EClass" name="Food">
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="name"
                    eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
              </eClassifiers>
              <eClassifiers xsi:type="ecore:EClass" name="Item" abstract="true"/>
              <eClassifiers xsi:type="ecore:EDataType" name="Code" instanceClassName="java.lang.String"/>
              <eSubpackages name="sub" nsURI="http://example.org/supplier/a/sub" nsPrefix="sub">
                <eClassifiers xsi:type="ecore:EClass" name="Part"/>
              </eSubpackages>
            """);
        writeMetaModel("supplier-b", """
            name="supplier" nsURI="http://example.org/supplier/b" nsPrefix="supplierb">
              <eClassifiers xsi:type="ecore:EClass" name="Food"/>
              <eClassifiers xsi:type="ecore:EDataType" name="Code" instanceClassName="java.lang.String"/>
            """);
        writeMetaModel("holder", """
            name="holder" nsURI="http://example.org/holder" nsPrefix="holder">
              <eClassifiers xsi:type="ecore:EClass" name="Holder"
                  eSuperTypes="http://www.eclipse.org/emf/2002/Ecore#//ENamedElement">
                <eStructuralFeatures xsi:type="ecore:EAttribute" name="label"
                    eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
                <eStructuralFeatures xsi:type="ecore:EReference" name="target"
                    eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
              </eClassifiers>
            """);
        writeMetaModel("units", """
            name="units" nsURI="http://example.org/units" nsPrefix="units">
              <eClassifiers xsi:type="ecore:EEnum" name="Unit">
                <eLiterals name="Piece"/>
              </eClassifiers>
            """);
    }

    private void writeMetaModel(String fileName, String packageAndClassifiers) throws IOException {
        Files.writeString(directory.resolve(fileName + ".ecore"), XMI_HEADER + "    " + packageAndClassifiers
            + "</ecore:EPackage>\n");
    }

    private String generate(Mode mode, String imports, String body) throws IOException {
        return generate(model -> mode, "package", imports, body);
    }

    /**
     * Generates the diagram with the mode that the given function returns for the generated view type, like the
     * language server does for {@link Mode.Selected}.
     */
    private String generate(Function<ModelInfo, Mode> getMode, String exportName, String imports, String body)
        throws IOException {
        var registry = new PackageModelCollector(directory.toString()).collect().right();
        var query = directory.resolve("query.nj");
        Files.writeString(query, """
            export %s to "http://example.com"

            %s

            %s
            """.formatted(exportName, imports, body));

        // Xtext keeps the first validator registered per JVM, which would use the registry of another test
        EValidator.Registry.INSTANCE.remove(AstPackage.eINSTANCE);
        var result = new NeoJoinStandaloneSetup(registry).getParser().parse(URI.createFileURI(query.toString()));
        assertThat(result).isInstanceOf(Parser.Result.Success.class);

        var aqr = ((Parser.Result.Success) result).aqr();
        var model = new MetaModelGenerator(aqr).generate();
        return new VisualizationGenerator(aqr, model, getMode.apply(model), false).generate();
    }

    @Test
    void fullModeShowsReferencedClassifiersOfNotImportedPackagesAsEmptyBoxes() throws IOException {
        var puml = generate(new Mode.Full(), ORDER_IMPORT, "from Order o create { o.fromA }");

        assertThat(puml).containsSubsequence(
            // external packages are declared before the lines that use their classes
            "package \"supplier (http://example.org/supplier/a)\" as 1external <<external>> {",
            "package \"supplier (http://example.org/supplier/b)\" as 2external <<external>> {",
            "class order.Order {",
            // attribute types are qualified only if needed: both supplier packages have a data type Code
            "unit : Unit",
            "code : supplier.Code (http://example.org/supplier/b)",
            "class 1external.Food {",
            "order.Order --> \"0..1\" 1external.Food : fromA",
            "class 1external.sub.Part {",
            "order.Order --> \"0..1\" 1external.sub.Part : part",
            "class 2external.Food {",
            "order.Order --> \"0..1\" 2external.Food : fromB",
            "abstract class 1external.Item {",
            "order.Order -up-|> 1external.Item"
        )
            // attribute types are text and get no box
            .doesNotContain("Code {").doesNotContain("Unit {").doesNotContain("external.Code")
            .doesNotContain("external.Unit");
    }

    @Test
    void referencedModeShowsSourceClassInNotImportedPackageWithArrowToTargetClass() throws IOException {
        var puml = generate(new Mode.Referenced(), ORDER_IMPORT, "from Order o create { o.fromA }");

        assertThat(puml).containsSubsequence(
            "package \"supplier (http://example.org/supplier/a)\" as 1external <<external>> {",
            "class 1external.Food {",
            "1external.Food .down.> 0target.Food"
        )
            // used by a reference and as source class, but declared once
            .containsOnlyOnce("class 1external.Food {");
    }

    @Test
    void selectedModeShowsSourceClassInNotImportedPackageOfSelectedClass() throws IOException {
        var puml = generate(
            model -> new Mode.Selected(List.of((EClass) model.pack().getEClassifier("Food"))),
            "package",
            ORDER_IMPORT,
            "from Order o create { o.fromA }"
        );

        // only one package named supplier is shown, so its title needs no namespace URI
        assertThat(puml).containsSubsequence(
            "package \"supplier\" as 1external <<external>> {",
            "class 1external.Food {",
            "1external.Food .down.> 0target.Food"
        )
            .doesNotContain("class 0target.Order");
    }

    @Test
    void externalPackageGetsNamespaceUriInTitleWhenImportAliasHasSameName() throws IOException {
        var puml = generate(new Mode.Referenced(), ORDER_IMPORT + " as units", "from Order o create { o.unit }");

        assertThat(puml).containsSubsequence(
            "package \"units (http://example.org/units)\" as 1external <<external>> {",
            "enum 1external.Unit <<enum>> {",
            "1external.Unit .down.> 0target.Unit"
        );
    }

    @Test
    void attributeTypeOfTargetPackageIsWrittenWithSimpleName() throws IOException {
        var puml = generate(new Mode.Referenced(), ORDER_IMPORT, "from Order o create { o.unit }");

        // the generator writes "\n" on every platform
        assertThat(puml).contains("class 0target.Order {\nunit : Unit\n}");
    }

    @Test
    void ecoreClassesAreEmptyBoxesInExternalPackage() throws IOException {
        var puml = generate(
            new Mode.Full(), "import \"http://example.org/holder\"", "from Holder h create { h.label h.target }"
        );

        assertThat(puml).containsSubsequence(
            "package \"ecore\" as 1external <<external>> {",
            "class holder.Holder {",
            "label : EString",
            "class 1external.EObject {",
            "holder.Holder --> \"0..1\" 1external.EObject : target",
            "abstract class 1external.ENamedElement {",
            "holder.Holder -up-|> 1external.ENamedElement"
        )
            // super types of Ecore classes are not expanded
            .doesNotContain("EModelElement").doesNotContain("EAnnotation");
    }

    @Test
    void targetPackageIsDistinctFromImportedPackageWithSameName() throws IOException {
        var puml = generate(
            model -> new Mode.Referenced(), "order", ORDER_IMPORT, "from Order o create { o.fromA }"
        );

        assertThat(puml).containsSubsequence(
            "package \"Target: order\" as 0target {",
            "class 0target.Order {",
            "class order.Order {",
            "order.Order .down.> 0target.Order"
        );
    }

    @Test
    void sourceClassAndEnumerationAreDrawnOnceWhenReachedSeveralTimes() throws IOException {
        var puml = generate(
            new Mode.Full(), ORDER_IMPORT + "\nimport \"http://example.org/units\"", "from Order o create { o.unit }"
        );

        // `Order` is a source class of a target class, `Unit` is an attribute type and a source enumeration
        assertThat(puml).containsOnlyOnce("class order.Order {").containsOnlyOnce("enum units.Unit <<enum>> {");
    }

    @Test
    void targetClassIsDrawnOnceWhenItIsAlsoSuperClass() throws IOException {
        var puml = generate(
            new Mode.Full(),
            "import \"http://example.org/supplier/a\"",
            "from Food create\n\nfrom Food f create Cheap extends Food { name = f.name }"
        );

        assertThat(puml).containsOnlyOnce("class 0target.Food {");
    }

    @Test
    void importedPackagesAreDrawnWithoutExternalPackages() throws IOException {
        var puml = generate(
            new Mode.Full(), "import \"http://example.org/supplier/a\"", "from Food f create { f.name }"
        );

        assertThat(puml).contains("class supplier.Food {").doesNotContain("<<external>>");
    }
}
