package tools.vitruv.neojoin;

import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmVisibility;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;

public class DefaultQueryModelExpressionTypeConfiguration implements QueryModelExpressionTypeConfiguration {
    @Override
    public void configure(JvmGenericType queryModelExpressionType, ViewTypeDefinition viewType) {
        queryModelExpressionType.setSimpleName("AllQueryExpressions");
        queryModelExpressionType.setVisibility(JvmVisibility.PRIVATE);

        // prevent name collisions with other open query documents by choosing a unique name for the package
        if (viewType.getExport() != null) {
            queryModelExpressionType.setPackageName(viewType.getExport().getPackage());
        } else {
            queryModelExpressionType.setPackageName("invalid$%d".formatted(System.identityHashCode(viewType)));
        }
    }
}
