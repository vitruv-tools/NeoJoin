package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.transformation.generated.ShopModel;
import tools.vitruv.neojoin.transformation.predicates.InstancePredicates;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

/**
 * Source models whose instances are generated EMF classes, not {@link DynamicEObjectImpl dynamic objects}.
 * Expressions and conditions must still read their features.
 */
public class GeneratedModelTest extends AbstractTransformationTest implements InstancePredicates {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of();
    }

    @Override
    protected List<String> getInstanceModelPaths() {
        return List.of();
    }

    @Override
    protected EPackage.Registry createPackageRegistry() {
        var registry = new EPackageRegistryImpl();
        registry.put(ShopModel.NS_URI, ShopModel.PACKAGE);
        return registry;
    }

    @Override
    protected Map<EPackage, Resource> createInstanceModelRegistry() {
        var shop = new ShopModel.Shop();
        shop.setName("Corner Shop");
        shop.getItems().add(item("Apple", 1));
        shop.getItems().add(item("Bread", 3));
        shop.getItems().add(item("Cheese", 7));
        shop.getItems().add(item(null, 0));

        org.assertj.core.api.Assertions.assertThat(shop).isNotInstanceOf(DynamicEObjectImpl.class);

        var resource = new ResourceSetImpl().createResource(URI.createURI("shop.xmi"));
        resource.getContents().add(shop);
        return Map.of(ShopModel.PACKAGE, resource);
    }

    private static ShopModel.Item item(@Nullable String name, int price) {
        var item = new ShopModel.Item();
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    private EObject transform(String query) {
        return internalTransform("""
            export package to "http://example.com"

            import "http://example.org/generatedshop"

            """ + query);
    }

    @Test
    void copiedFeatures() {
        var result = transform("""
            from Item i where i.name !== null create { i.name i.price }
            """);

        assertThat(result)
            .hasInstance("Item", and(named("Apple"), attribute("price", 1)))
            .hasInstance("Item", and(named("Bread"), attribute("price", 3)))
            .hasInstance("Item", and(named("Cheese"), attribute("price", 7)))
            .hasNoMoreInstances();
    }

    @Test
    void condition() {
        var result = transform("""
            from Item i where i.price > 2 create { i.name }
            """);

        assertThat(result)
            .hasInstance("Item", named("Bread"))
            .hasInstance("Item", named("Cheese"))
            .hasNoMoreInstances();
    }

    @Test
    void expression() {
        var result = transform("""
            from Item i where i.name !== null create {
                i.name
                label := i.name.toUpperCase + " " + i.price
            }
            """);

        assertThat(result)
            .hasInstance("Item", named("Apple"), item -> assertThat(item).hasAttribute("label", "APPLE 1"))
            .hasInstance("Item", named("Bread"), item -> assertThat(item).hasAttribute("label", "BREAD 3"))
            .hasInstance("Item", named("Cheese"), item -> assertThat(item).hasAttribute("label", "CHEESE 7"))
            .hasNoMoreInstances();
    }

    @Test
    void referenceNavigation() {
        var result = transform("""
            from Shop s create {
                s.name
                itemCount := s.items.size
                priciest := s.items.maxBy[price].name
                total := s.items.map[price].reduce[a, b | a + b]
            }
            """);

        assertThat(result)
            .hasInstance(
                "Shop", named("Corner Shop"), shop ->
                    assertThat(shop)
                        .hasAttribute("itemCount", 4)
                        .hasAttribute("priciest", "Cheese")
                        .hasAttribute("total", 11)
            )
            .hasNoMoreInstances();
    }

    @Test
    void unsetAttributeIsNull() {
        var result = transform("""
            from Item i where i.name === null create { i.price }
            """);

        assertThat(result)
            .hasInstance("Item", attribute("price", 0))
            .hasNoMoreInstances();
    }

    @Test
    void nullReceiverFails() {
        assertThatThrownBy(() ->
            transform("""
                from Item i create { len := i.name.length }
                """)
        ).hasMessageStartingWith("Failed to transform models");
    }

}
