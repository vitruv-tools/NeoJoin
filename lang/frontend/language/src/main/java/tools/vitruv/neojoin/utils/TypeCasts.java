package tools.vitruv.neojoin.utils;

import com.google.common.primitives.Primitives;

import static tools.vitruv.neojoin.utils.Assertions.check;
import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Contains static methods for casting java types.
 */
public final class TypeCasts {

    /**
     * Checks whether a cast from one java class to another is possible.
     * @param from source type
     * @param to target type
     * @return true if the cast is possible, false otherwise
     */
    public static boolean canCast(Class<?> from, Class<?> to) {
        //noinspection ConstantValue - sanity check because `type.getInstanceClass()` can return null which is not caught by nullability checks
        check(from != null && to != null);
        var wrappedFrom = Primitives.wrap(from);
        var wrappedTo = Primitives.wrap(to);
        check(wrappedFrom != Void.class && wrappedTo != Void.class);

        if (wrappedFrom == wrappedTo) {
            return true;
        }

        if (!Primitives.isWrapperType(wrappedFrom) || !Primitives.isWrapperType(wrappedTo)) {
            // fallback to java assignment semantics for non-primitive types
            return wrappedTo.isAssignableFrom(wrappedFrom);
        }

        // booleans cannot be cast
        return wrappedFrom != Boolean.class && wrappedTo != Boolean.class;
    }

    /**
     * Casts the given value to the given type.
     * @param value the value to cast
     * @param to the type to cast to
     * @return the cast value
     */
    public static Object cast(Object value, Class<?> to) {
        check(canCast(value.getClass(), to));
        var wrappedTo = Primitives.wrap(to);

        if (!Primitives.isWrapperType(wrappedTo)) {
            // fallback to java assignment semantics for non-primitive types
            return value;
        }

        if (wrappedTo == value.getClass()) {
            // no conversion required, also completely handles booleans
            return value;
        }

        check(value.getClass() != Boolean.class && wrappedTo != Boolean.class);

        // number to character
        if (wrappedTo == Character.class) {
            // short has the same size as char
            return (char) ((Number) value).shortValue();
        }

        // character to number
        if (value instanceof Character character) {
            char c = character;
            if (wrappedTo == Byte.class) {
                return (byte) c;
            } else if (wrappedTo == Short.class) {
                return (short) c;
            } else if (wrappedTo == Integer.class) {
                return (int) c;
            } else if (wrappedTo == Long.class) {
                return (long) c;
            } else if (wrappedTo == Float.class) {
                return (float) c;
            } else if (wrappedTo == Double.class) {
                return (double) c;
            } else {
                return fail(); // should not occur
            }
        }

        // number to number
        var number = (Number) value;
        if (wrappedTo == Byte.class) {
            return number.byteValue();
        } else if (wrappedTo == Short.class) {
            return number.shortValue();
        } else if (wrappedTo == Integer.class) {
            return number.intValue();
        } else if (wrappedTo == Long.class) {
            return number.longValue();
        } else if (wrappedTo == Float.class) {
            return number.floatValue();
        } else if (wrappedTo == Double.class) {
            return number.doubleValue();
        } else {
            return fail(); // should not occur
        }
    }

}
