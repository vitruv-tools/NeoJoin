package tools.vitruv.neojoin.utils;

import com.google.common.primitives.Primitives;

import static tools.vitruv.neojoin.utils.Assertions.*;

/**
 * Contains static methods for casting java types.
 */
public final class TypeCasts {

    private TypeCasts() {
    }

    /**
     * Checks whether a cast from one java class to another is possible.
     *
     * @param from source type
     * @param to   target type
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
     *
     * @param value the value to cast
     * @param to    the type to cast to
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

        if (wrappedTo == Character.class) {
            return castNumberToChar(value, wrappedTo);
        }

        if (value instanceof Character character) {
            return castCharToNumber(character, wrappedTo);
        }

        return castNumberToNumber(value, wrappedTo);
    }

    private static Character castNumberToChar(Object value, Class<?> to) {
        // short has the same size as char
        return (char) ((Number) value).shortValue();
    }

    private static Number castNumberToNumber(Object value, Class<?> to) {
        var number = (Number) value;
        if (to == Byte.class) {
            return number.byteValue();
        } else if (to == Short.class) {
            return number.shortValue();
        } else if (to == Integer.class) {
            return number.intValue();
        } else if (to == Long.class) {
            return number.longValue();
        } else if (to == Float.class) {
            return number.floatValue();
        } else if (to == Double.class) {
            return number.doubleValue();
        } else {
            return fail(); // should not occur
        }
    }

    private static Number castCharToNumber(Character character, Class<?> to) {
        char c = character;
        if (to == Byte.class) {
            return (byte) c;
        } else if (to == Short.class) {
            return (short) c;
        } else if (to == Integer.class) {
            return (int) c;
        } else if (to == Long.class) {
            return (long) c;
        } else if (to == Float.class) {
            return (float) c;
        } else if (to == Double.class) {
            return (double) c;
        } else {
            return fail(); // should not occur
        }
    }
}
