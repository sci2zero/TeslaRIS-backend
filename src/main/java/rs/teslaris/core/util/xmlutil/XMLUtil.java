package rs.teslaris.core.util.xmlutil;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class XMLUtil {

    public static <T> String convertToXml(T object) throws JAXBException {
        var context = JAXBContext.newInstance(object.getClass());
        var marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // Pretty print XML

        var writer = new StringWriter();
        marshaller.marshal(object, writer);
        return writer.toString();
    }

    public static void cleanXmlObject(Object obj) {
        cleanXmlObject(obj, new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    private static void cleanXmlObject(Object obj, Set<Object> visited) {
        if (Objects.isNull(obj)) {
            return;
        }

        if (!visited.add(obj)) {
            return;
        }

        Class<?> clazz = obj.getClass();

        if (clazz.isPrimitive() ||
            clazz.getName().startsWith("java") ||
            clazz.isEnum()) {
            return;
        }

        for (var field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (Objects.isNull(value)) {
                    continue;
                }

                if (value instanceof String str) {
                    if (str.isBlank()) {
                        field.set(obj, null);
                    }
                    continue;
                }

                if (value instanceof Collection<?> collection) {
                    if (collection.isEmpty()) {
                        field.set(obj, null);
                    } else {
                        for (Object element : collection) {
                            cleanXmlObject(element, visited);
                        }
                    }
                } else if (value instanceof Map<?, ?> map) {
                    if (map.isEmpty()) {
                        field.set(obj, null);
                    } else {
                        map.values().forEach(v -> cleanXmlObject(v, visited));
                    }
                } else if (value.getClass().isArray()) {
                    int len = Array.getLength(value);
                    if (len == 0) {
                        field.set(obj, null);
                    } else {
                        for (int i = 0; i < len; i++) {
                            cleanXmlObject(Array.get(value, i), visited);
                        }
                    }
                } else {
                    cleanXmlObject(value, visited);
                }

            } catch (IllegalAccessException ignored) {
                // pass
            }
        }
    }
}
