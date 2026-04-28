package tools.vitruv.neojoin.expression_parser.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;

/**
 * FindAny selects any element from a collection of elements. There are no guarantees which element
 * will be selected
 *
 * <p>Example expressions may look like
 *
 * <pre>
 *     {@code
 *          someResult = car.axis.findFirst()
 *          someResult = car.axis.findLast()
 *     }
 * </pre>
 *
 * Here, {@code X.findFirst()} and {@code X.findLast()} are FindAny operations
 */
@Data
@RequiredArgsConstructor
public class FindAny implements ReferenceOperator {
    @Nullable ReferenceOperator followingOperator;
}
