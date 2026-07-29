package tools.vitruv.neojoin;

import com.google.inject.ImplementedBy;
import org.eclipse.xtext.common.types.JvmGenericType;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;

@ImplementedBy(DefaultQueryModelExpressionTypeConfiguration.class)
public interface QueryModelExpressionTypeConfiguration {
    void configure(JvmGenericType queryModelExpressionType, ViewTypeDefinition viewType);
}
