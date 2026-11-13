package tools.vitruv.neojoin.ide.custom;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.findReferences.TargetURIs;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.JvmModelTargetURICollector;

/**
 * Searches the references of an inferred JVM element as references of the query element it was inferred from.
 * <p>
 * Each feature expression of a query is inferred as a method of its own (see {@code QueryModelInferrer}), with a
 * parameter for each variable ({@code r} of {@code from Restaurant r}) and query parameter. So the uses of a variable
 * in two features refer to two different parameters, and by default a references request on one use lists only that
 * use.
 * Xbase already adds all inferred elements when the search starts at the query element; this adds them also when it
 * starts at one of the inferred elements.
 */
public class SourceElementTargetURICollector extends JvmModelTargetURICollector {

    @Inject
    private IJvmModelAssociations associations;

    @Override
    protected void doAdd(EObject object, TargetURIs targetURIs) {
        if (object instanceof JvmIdentifiableElement) {
            var source = associations.getPrimarySourceElement(object);
            if (source != null && !(source instanceof JvmIdentifiableElement)) {
                super.doAdd(source, targetURIs); // also adds all elements inferred from the source, including object
                return;
            }
        }
        super.doAdd(object, targetURIs);
    }

}
