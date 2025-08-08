package tools.vitruv.neojoin.ide.custom;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.typesystem.internal.FeatureScopeTracker;
import org.eclipse.xtext.xbase.typesystem.internal.IFeatureScopeTracker;
import org.eclipse.xtext.xbase.typesystem.internal.OptimizingFeatureScopeTrackerProvider;

/**
 * Workaround for <a href="https://github.com/eclipse-xtext/xtext/issues/2359">#2359</a>.
 * From <a href="https://github.com/LorenzoBettini/edelta/commit/6809402fd4f66a48188307bfe2ec5b4c22bb0ec4">6809402f</a>.
 */
public class OptimizingFeatureScopeTrackerProvider2 extends OptimizingFeatureScopeTrackerProvider {

    @Override
    public IFeatureScopeTracker track(EObject root) {
        return new FeatureScopeTracker() {};
    }

}
