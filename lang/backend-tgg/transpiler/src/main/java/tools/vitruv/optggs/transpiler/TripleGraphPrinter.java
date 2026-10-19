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

public class TripleGraphPrinter {
    public static void print(TripleGrammar grammar, Path file, NameResolver nameResolver) {
        // Preprocess grammar for eMoflon
        EmoflonTripleGraphGrammarPreprocessor.preprocess(grammar);

        CodeResolver codeResolver =
                new ResourceCodeResolver("tools/vitruv/optggs/transpiler/templates");
        TemplateEngine engine = TemplateEngine.create(codeResolver, ContentType.Plain);
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
