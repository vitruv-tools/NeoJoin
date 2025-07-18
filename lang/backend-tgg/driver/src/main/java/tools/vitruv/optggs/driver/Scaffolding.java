package tools.vitruv.optggs.driver;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.FileOutput;
import gg.jte.resolve.ResourceCodeResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Scaffolding {
    private final Path targetPath;

    public Scaffolding(Path targetPath) {
        this.targetPath = targetPath;
    }

    public Path transformationFilePath() {
        return targetPath.resolve("src/Transform.msl");
    }

    public void create(Project project) {
        var path = this.targetPath;
        CodeResolver codeResolver = new ResourceCodeResolver("tools/vitruv/optggs/driver/templates");
        TemplateEngine engine = TemplateEngine.create(codeResolver, ContentType.Plain);
        createFile(path.resolve(".project"), "project", project, engine);
        createFile(path.resolve(".classpath"), "classpath", project, engine);
        createFile(path.resolve("build.properties"), "build_properties", project, engine);
        createFile(path.resolve("META-INF/MANIFEST.MF"), "manifest", project, engine);
        try {
            Files.createDirectories(path.resolve("src"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mergeFiles(path.resolve("src/SourceMetamodel.msl"), project.sourceMetamodels().stream().map(Metamodel::path).toList());
        mergeFiles(path.resolve("src/TargetMetamodel.msl"), project.targetMetamodels().stream().map(Metamodel::path).toList());

        for (var sourceModel : project.sourceModels()) {
            System.out.println("Generate source model " + sourceModel.name());
            var params = new HashMap<String, Object>();
            params.put("project", project);
            params.put("source", sourceModel.name());
            copyModel(path.resolve("src/Source" + sourceModel.name() + ".msl"), sourceModel, project);
            createFile(path.resolve("src/" + project.name() + sourceModel.name() + "ForwardRunner.java"), "ScopedForwardRunner", params, engine);
        }

        for (var targetModel : project.targetModels()) {
            System.out.println("Generate target model " + targetModel.name());
            var params = new HashMap<String, Object>();
            params.put("project", project);
            params.put("target", targetModel.name());
            copyModel(path.resolve("src/Target" + targetModel.name() + ".msl"), targetModel, project);
            createFile(path.resolve("src/" + project.name() + targetModel.name() + "BackwardRunner.java"), "ScopedBackwardRunner", params, engine);
        }

        for (var syncModel : project.syncModels()) {
            var params = new HashMap<String, Object>();
            params.put("project", project);
            params.put("name", syncModel.name());
            params.put("source", syncModel.name() + "_Source");
            params.put("target", syncModel.name() + "_Target");
            copyModel(path.resolve("src/Sync" + syncModel.name() + ".msl"), syncModel, project);
            createFile(path.resolve("src/" + project.name() + syncModel.name() + "SyncRunner.java"), "ScopedSyncRunner", params, engine);
        }

        createFile(path.resolve("src/" + project.name() + "ForwardRunner.java"), "ForwardRunner", project, engine);
        createFile(path.resolve("src/" + project.name() + "BackwardRunner.java"), "BackwardRunner", project, engine);
        createFile(path.resolve("src/" + project.name() + "SyncRunner.java"), "SyncRunner", project, engine);
        createFile(path.resolve("src/RegisterAttributeConstraints.java"), "RegisterAttributeConstraints", project, engine);
        createFile(path.resolve("src/AttributeConstraints.msl"), "AttributeConstraints", project, engine);
        for (var solver : project.constraintSolvers()) {
            try {
                solver.copyFile(path.resolve("src"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createFile(Path path, String template, Project project, TemplateEngine engine) {
        try (FileOutput out = new FileOutput(path)) {
            engine.render(template + ".jte", project, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFile(Path path, String template, Map<String, Object> params, TemplateEngine engine) {
        try (FileOutput out = new FileOutput(path)) {
            engine.render(template + ".jte", params, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mergeFiles(Path targetFile, Collection<Path> paths) {
        try (FileWriter writer = new FileWriter(targetFile.toFile())) {
            for (var path : paths) {
                appendFile(path, writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendFile(Path path, FileWriter writer) throws IOException {
        try (Scanner reader = new Scanner(path.toFile())) {
            while (reader.hasNextLine()) {
                writer.write(reader.nextLine());
                writer.write("\n");
            }
        }
    }

    private static void copyModel(Path targetFile, Model model, Project project) {
        try (FileWriter writer = new FileWriter(targetFile.toFile())) {
            writer.write("import \"platform:/resource/" + project.name() + "/src/SourceMetamodel.msl\"\n");
            writer.write("import \"platform:/resource/" + project.name() + "/src/TargetMetamodel.msl\"\n");
            try (Scanner reader = new Scanner(model.path().toFile())) {
                while (reader.hasNextLine()) {
                    writer.write(reader.nextLine());
                    writer.write("\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
