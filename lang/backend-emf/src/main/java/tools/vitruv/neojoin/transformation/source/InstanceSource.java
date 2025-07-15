package tools.vitruv.neojoin.transformation.source;

import tools.vitruv.neojoin.transformation.InstanceTuple;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An instance source provides a stream of {@link InstanceTuple instance tuples}.
 *
 * @see #get()
 */
public interface InstanceSource extends Supplier<Stream<InstanceTuple>> {}
