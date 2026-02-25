package rs.teslaris.importer.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeepObjectMerger {

    private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
        String.class,
        Integer.class,
        Long.class,
        Double.class,
        Float.class,
        Boolean.class,
        Character.class,
        Byte.class,
        Short.class,
        BigDecimal.class,
        BigInteger.class
    );


    public static <T> T deepMerge(T primary, T secondary) {
        if (primary == null || secondary == null) {
            throw new IllegalArgumentException("Objects must not be null");
        }

        Class<?> clazz = primary.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {

                if (Modifier.isStatic(field.getModifiers())
                    || Modifier.isFinal(field.getModifiers())
                    || field.getType().isPrimitive()) {
                    continue;
                }

                field.setAccessible(true);

                try {
                    Object primaryValue = field.get(primary);
                    Object secondaryValue = field.get(secondary);

                    if (secondaryValue == null) {
                        continue;
                    }

                    if (primaryValue == null) {
                        field.set(primary, secondaryValue);
                        continue;
                    }

                    if (primaryValue instanceof Collection<?> pCol
                        && secondaryValue instanceof Collection<?> sCol) {

                        if (pCol.size() < sCol.size()) {
                            field.set(primary, secondaryValue);
                        } else if (pCol.size() == sCol.size()) {
                            mergeCollections(pCol, sCol);
                        }
                        continue;
                    }

                    if (isMergeable(primaryValue)) {
                        deepMerge(primaryValue, secondaryValue);
                    }

                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                        "Failed to access field: " + field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return primary;
    }

    private static void mergeCollections(Collection<?> primary, Collection<?> secondary) {
        if (primary instanceof List<?> pList && secondary instanceof List<?> sList) {
            for (int i = 0; i < pList.size(); i++) {
                Object p = pList.get(i);
                Object s = sList.get(i);
                if (p != null && s != null && isMergeable(p)) {
                    deepMerge(p, s);
                }
            }
        }
    }

    private static boolean isMergeable(Object value) {
        Class<?> type = value.getClass();
        return !IMMUTABLE_TYPES.contains(type)
            && !Enum.class.isAssignableFrom(type)
            && !Date.class.isAssignableFrom(type)
            && !Map.class.isAssignableFrom(type)
            && !type.isArray();
    }
}
