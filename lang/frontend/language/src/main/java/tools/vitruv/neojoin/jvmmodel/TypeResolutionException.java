package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.xtext.xbase.XExpression;

public class TypeResolutionException extends Exception {

    public TypeResolutionException(XExpression expression) {
        super("Failed to resolve expression type of: " + expression);
    }

}
