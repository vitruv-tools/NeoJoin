package tools.vitruv.neojoin.ide.visualization;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.klimt.color.ColorMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Renders PlantUML source code.
 */
public class PlantUMLRenderer {

    private final String source;
    private final FileFormat format;
    private final boolean darkMode;

    public PlantUMLRenderer(String source, FileFormat format, boolean darkMode) {
        this.source = source;
        this.format = format;
        this.darkMode = darkMode;
    }

    public void renderTo(OutputStream out) throws IOException {
        var reader = new SourceStringReader(source);
        var outputFormat = new FileFormatOption(format);
        if (darkMode) {
            outputFormat = outputFormat.withColorMapper(ColorMapper.DARK_MODE);
        }
        reader.outputImage(out, outputFormat);
    }

    public String renderToString() {
        var out = new ByteArrayOutputStream();
        try {
            renderTo(out);
        } catch (IOException e) {
            // should not occur because we do not use the file system
            throw new RuntimeException(e);
        }
        return out.toString();
    }

}
