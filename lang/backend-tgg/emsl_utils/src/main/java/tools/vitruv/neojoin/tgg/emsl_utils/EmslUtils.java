package tools.vitruv.neojoin.tgg.emsl_utils;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EmslUtils {
    private static final Set<String> EMSL_KEYWORDS =
            Set.of("target", "abstract", "attributeConstraints", "pattern", "source", "rules");

    public static String escapeKeywords(String str) {
        if (str == null) return "???";

        if (isEmslKeyword(str)) return "^" + str;

        return str;
    }

    private static boolean isEmslKeyword(String str) {
        return EMSL_KEYWORDS.contains(str);
    }

    public static String extractModelName(URI uri) {
        var filename = uri.segment(uri.segmentCount() - 1);
        return filename.substring(0, filename.length() - 4);
    }

    public static Map<EObject, String> createObjectIDs(ResourceSet rs) {
        Map<EObject, String> objIDs = new HashMap<>();
        TreeIterator<?> it = rs.getAllContents();
        while (it.hasNext()) {
            Object next = it.next();
            if (next instanceof EObject e) {
                objIDs.put(e, "o" + objIDs.size());
            }
        }
        return objIDs;
    }

    public static String getBuiltInValue(EAttribute attr, EObject modelElt) {
        Object value = modelElt.eGet(attr);
        String typeNameEsc = escapeKeywords(attr.getEType().getName());
        if ("EString".equals(typeNameEsc) || "EChar".equals(typeNameEsc)) {
            return "\"" + value + "\"";
        }
        return value.toString();
    }

    public static String registerEnum(Map<String, EEnum> metaModelEnum, EEnum eenum) {
        metaModelEnum.put(eenum.getName(), eenum);
        return eenum.getName();
    }

    public static String getUserDefinedValue(
            Map<String, EEnum> metaModelEnum, EAttribute attr, EObject modelElt) {

        if (attr.getEType() == null) return null;
        EEnum eEnum = metaModelEnum.get(attr.getEType().getName());
        if (eEnum == null) return null;

        Object raw = modelElt.eGet(attr);
        if (raw == null) return null;

        EEnumLiteral lit = eEnum.getEEnumLiteralByLiteral(raw.toString());
        if (lit == null) return null;

        return escapeKeywords(lit.getName());
    }
}
