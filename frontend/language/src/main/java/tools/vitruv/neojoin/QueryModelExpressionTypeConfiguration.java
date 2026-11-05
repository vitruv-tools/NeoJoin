package tools.vitruv.neojoin;

import com.google.inject.ImplementedBy;
import org.eclipse.xtext.common.types.JvmGenericType;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;

/**
 * Configures the type holding all methods which are created for the expressions in the query.
 */
@ImplementedBy(DefaultQueryModelExpressionTypeConfiguration.class)
public interface QueryModelExpressionTypeConfiguration {
    void configure(JvmGenericType queryModelExpressionType, ViewTypeDefinition viewType);
}
