package tools.vitruv.neojoin.ide;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.util.IFileSystemScanner;

import java.io.File;
import java.util.Set;

/**
 * Skips directories with the given names (e.g. build output and VCS directories) when the language server indexes a
 * workspace folder.
 * <p>
 * Without this, a Maven build that copies {@code .nj} files into {@code target/classes} makes the server index both
 * the source and its copy, and every query of the copy is reported as "already defined".
 */
public class NeoJoinFileSystemScanner extends IFileSystemScanner.JavaIoFileSystemScanner {

    private final Set<String> excludedDirectories;

    public NeoJoinFileSystemScanner(Set<String> excludedDirectories) {
        this.excludedDirectories = Set.copyOf(excludedDirectories);
    }

    @Override
    public void scanRec(File file, IAcceptor<URI> acceptor) {
        if (file.isDirectory() && excludedDirectories.contains(file.getName())) {
            return;
        }
        super.scanRec(file, acceptor);
    }

}
