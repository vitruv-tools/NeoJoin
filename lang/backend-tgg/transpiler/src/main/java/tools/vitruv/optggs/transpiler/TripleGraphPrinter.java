package tools.vitruv.optggs.transpiler;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.FileOutput;
import gg.jte.resolve.ResourceCodeResolver;
import tools.vitruv.optggs.transpiler.emoflon.EmoflonTripleGraphGrammarPreprocessor;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleGrammar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public final class TripleGraphPrinter {
    /**
     * The directory in which the template engine caches the compiled templates, if no other
     * directory is given. It is relative to the working directory.
     */
    public static final Path DEFAULT_TEMPLATE_CACHE_DIRECTORY = Path.of("jte-classes");

    private TripleGraphPrinter() {}

    public static void print(TripleGrammar grammar, Path file, NameResolver nameResolver) {
        print(grammar, file, nameResolver, DEFAULT_TEMPLATE_CACHE_DIRECTORY);
    }

    public static void print(
            TripleGrammar grammar, Path file, NameResolver nameResolver, Path templateCacheDirectory) {
        // Preprocess grammar for eMoflon
        EmoflonTripleGraphGrammarPreprocessor.preprocess(grammar);

        CodeResolver codeResolver = new ResourceCodeResolver("tools/vitruv/optggs/transpiler/templates");
        TemplateEngine engine = TemplateEngine.create(codeResolver, templateCacheDirectory, ContentType.Plain);
        var params = new HashMap<String, Object>();
        params.put("grammar", grammar);
        params.put("nameResolver", nameResolver);
        try (FileOutput out = new FileOutput(file)) {
            engine.render("tgg/TripleGrammar.jte", params, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
