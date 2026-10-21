package tools.vitruv.neojoin.transformation.generated;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * Minimal stand-in for an EMF model generated from a genmodel: instances are hand-written
 * {@link MinimalEObjectImpl} subclasses that store their feature values in Java fields, like generated code,
 * instead of {@link org.eclipse.emf.ecore.impl.DynamicEObjectImpl dynamic objects}.
 */
public final class ShopModel {

    public static final String NS_URI = "http://example.org/generatedshop";

    public static final EPackage PACKAGE;
    private static final EClass SHOP;
    private static final EClass ITEM;

    private static final int SHOP__NAME = 0;
    private static final int SHOP__ITEMS = 1;
    private static final int ITEM__NAME = 0;
    private static final int ITEM__PRICE = 1;

    static {
        var factory = EcoreFactory.eINSTANCE;

        SHOP = factory.createEClass();
        SHOP.setName("Shop");
        SHOP.setInstanceClass(Shop.class);

        ITEM = factory.createEClass();
        ITEM.setName("Item");
        ITEM.setInstanceClass(Item.class);

        SHOP.getEStructuralFeatures().add(attribute("name", EcorePackage.Literals.ESTRING));
        EReference items = factory.createEReference();
        items.setName("items");
        items.setEType(ITEM);
        items.setContainment(true);
        items.setUpperBound(-1);
        SHOP.getEStructuralFeatures().add(items);

        ITEM.getEStructuralFeatures().add(attribute("name", EcorePackage.Literals.ESTRING));
        ITEM.getEStructuralFeatures().add(attribute("price", EcorePackage.Literals.EINT));

        PACKAGE = factory.createEPackage();
        PACKAGE.setName("generatedshop");
        PACKAGE.setNsPrefix("generatedshop");
        PACKAGE.setNsURI(NS_URI);
        PACKAGE.getEClassifiers().add(SHOP);
        PACKAGE.getEClassifiers().add(ITEM);
        PACKAGE.setEFactoryInstance(new EFactoryImpl() {
            @Override
            public EObject create(EClass eClass) {
                if (eClass == SHOP) {
                    return new Shop();
                } else if (eClass == ITEM) {
                    return new Item();
                }
                throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
            }
        });
        // like EPackageImpl.createResource(..) in generated packages
        new ResourceImpl(URI.createURI(NS_URI)).getContents().add(PACKAGE);
    }

    private ShopModel() {}

    private static EAttribute attribute(String name, org.eclipse.emf.ecore.EDataType type) {
        var attribute = EcoreFactory.eINSTANCE.createEAttribute();
        attribute.setName(name);
        attribute.setEType(type);
        return attribute;
    }

    public static class Shop extends MinimalEObjectImpl.Container {

        protected String name;
        protected EList<Item> items;

        @Override
        protected EClass eStaticClass() {
            return SHOP;
        }

        public String getName() {
            return name;
        }

        public void setName(String newName) {
            name = newName;
        }

        public EList<Item> getItems() {
            if (items == null) {
                items = new EObjectContainmentEList<>(Item.class, this, SHOP__ITEMS);
            }
            return items;
        }

        @Override
        public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
            if (featureID == SHOP__ITEMS) {
                return ((InternalEList<?>) getItems()).basicRemove(otherEnd, msgs);
            }
            return super.eInverseRemove(otherEnd, featureID, msgs);
        }

        @Override
        public Object eGet(int featureID, boolean resolve, boolean coreType) {
            return switch (featureID) {
                case SHOP__NAME -> getName();
                case SHOP__ITEMS -> getItems();
                default -> super.eGet(featureID, resolve, coreType);
            };
        }

        @Override
        public boolean eIsSet(int featureID) {
            return switch (featureID) {
                case SHOP__NAME -> name != null;
                case SHOP__ITEMS -> items != null && !items.isEmpty();
                default -> super.eIsSet(featureID);
            };
        }
    }

    public static class Item extends MinimalEObjectImpl.Container {

        protected String name;
        protected int price;

        @Override
        protected EClass eStaticClass() {
            return ITEM;
        }

        public String getName() {
            return name;
        }

        public void setName(String newName) {
            name = newName;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int newPrice) {
            price = newPrice;
        }

        @Override
        public Object eGet(int featureID, boolean resolve, boolean coreType) {
            return switch (featureID) {
                case ITEM__NAME -> getName();
                case ITEM__PRICE -> getPrice();
                default -> super.eGet(featureID, resolve, coreType);
            };
        }

        @Override
        public boolean eIsSet(int featureID) {
            return switch (featureID) {
                case ITEM__NAME -> name != null;
                case ITEM__PRICE -> price != 0;
                default -> super.eIsSet(featureID);
            };
        }
    }

}
