package tools.vitruv.optggs.transpiler.graph.tgg.constraint;

import tools.vitruv.optggs.transpiler.graph.tgg.Parameter;

import java.util.List;

public interface AttributeConstraint {
    String SELF_PARAMETER_NAME = "self";
    String RETURN_PARAMETER_NAME = "return";

    String getConstraintName();

    List<Parameter> getParameters();

    AttributeConstraint deepCopy();
}
