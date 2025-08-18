package rs.teslaris.importer.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DeepObjectMerger {

    private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
        String.class, Integer.class, Long.class, Double.class, Float.class,
        Boolean.class, Character.class, Byte.class, Short.class,
        java.math.BigDecimal.class, java.math.BigInteger.class
    );


    public static <T> T deepMerge(T primary, T secondary) {
        if (Objects.isNull(primary) || Objects.isNull(secondary)) {
            throw new IllegalArgumentException("Objects must not be null");
        }

        Class<?> clazz = primary.getClass();
        while (Objects.nonNull(clazz)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (IMMUTABLE_TYPES.contains(field.getType()) ||
                    field.getType().toString().equals("int") ||
                    field.getType().toString().equals("boolean")) {
                    continue; // don't reflect into immutable types
                }

                if (Modifier.isStatic(field.getModifiers()) ||
                    Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);

                try {
                    Object primaryValue = field.get(primary);
                    Object secondaryValue = field.get(secondary);

                    if (Objects.isNull(primaryValue) && Objects.nonNull(secondaryValue)) {
                        field.set(primary, secondaryValue);
                    } else if (isMergeable(primaryValue, secondaryValue)) {
                        // Recursively deep merge nested objects
                        deepMerge(primaryValue, secondaryValue);
                    } else if (primaryValue instanceof Collection<?> primaryCollection &&
                        secondaryValue instanceof Collection<?> secondaryCollection) {
                        if (primaryCollection.size() < secondaryCollection.size()) {
                            field.set(primary, secondaryValue);
                        } else if (primaryCollection.size() == secondaryCollection.size()) {
                            for (int i = 0; i < primaryCollection.size(); i++) {
                                deepMerge(((ArrayList<?>) primaryCollection).get(i),
                                    ((ArrayList<?>) secondaryCollection).get(i));
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access field: " + field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return primary;
    }

    private static boolean isMergeable(Object primaryValue, Object secondaryValue) {
        if (Objects.isNull(primaryValue) || Objects.isNull(secondaryValue)) {
            return false;
        }

        Class<?> type = primaryValue.getClass();
        return !type.isPrimitive()
            && !String.class.isAssignableFrom(type)
            && !Number.class.isAssignableFrom(type)
            && !Boolean.class.isAssignableFrom(type)
            && !Date.class.isAssignableFrom(type)
            && !Collection.class.isAssignableFrom(type)
            && !Map.class.isAssignableFrom(type)
            && !Enum.class.isAssignableFrom(type)
            && !type.isArray();
    }
}

