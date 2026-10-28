package tools.vitruv.neojoin.ide;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.util.IFileSystemScanner;

import java.io.File;
import java.util.Set;

/**
 * Skips build output and VCS directories when the language server indexes a workspace folder.
 * <p>
 * Without this, a Maven build that copies {@code .nj} files into {@code target/classes} makes the server index both
 * the source and its copy, and every query of the copy is reported as "already defined".
 */
public class NeoJoinFileSystemScanner extends IFileSystemScanner.JavaIoFileSystemScanner {

    static final Set<String> EXCLUDED_DIRECTORIES = Set.of("target", "bin", "build", "node_modules", ".git");

    @Override
    public void scanRec(File file, IAcceptor<URI> acceptor) {
        if (file.isDirectory() && EXCLUDED_DIRECTORIES.contains(file.getName())) {
            return;
        }
        super.scanRec(file, acceptor);
    }

}
