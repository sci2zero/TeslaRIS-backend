package rs.teslaris.migrator.pipeline.merge;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Merges the source DTO of a changed record into the DTO of the entity a previous run wrote.
 * <p>
 * A core edit method replaces the whole entity, so a sync run must not send the source DTO alone.
 * The merge therefore keeps the curated value, and it takes the source value only where the
 * curated value is empty, or where both values are text and the source text is longer.
 * Multilingual content follows the same rule per language tag. Each replaced text is one line in
 * {@code migration.log}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationMergePolicy {

    private static final String RULE = "MERGE";

    private final MigrationLog migrationLog;

    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();


    public <D> D merge(D source, D current, MergeContext context) {
        if (Objects.isNull(current)) {
            return source;
        }

        if (Objects.isNull(source)) {
            return current;
        }

        fieldsOf(source.getClass())
            .forEach(field -> mergeField(field, source, current, context));

        return current;
    }

    private void mergeField(Field field, Object source, Object current, MergeContext context) {
        try {
            var sourceValue = field.get(source);

            if (isEmpty(sourceValue)) {
                return;
            }

            var targetField = findField(current.getClass(), field.getName());

            if (Objects.isNull(targetField)) {
                return;
            }

            var currentValue = targetField.get(current);

            if (isEmpty(currentValue)) {
                targetField.set(current, sourceValue);
                return;
            }

            if (sourceValue instanceof String sourceText &&
                currentValue instanceof String currentText) {
                mergeText(targetField, current, sourceText, currentText, context);
                return;
            }

            if (isMultilingual(sourceValue) && isMultilingual(currentValue)) {
                targetField.set(current,
                    mergeMultilingual(asMultilingual(sourceValue), asMultilingual(currentValue),
                        field.getName(), context));
            }
        } catch (Exception e) {
            log.warn("Unable to merge field {} of {}.", field.getName(), context.key(), e);
        }
    }

    private void mergeText(Field targetField, Object current, String sourceText,
                           String currentText, MergeContext context) throws Exception {
        if (sourceText.trim().length() <= currentText.trim().length()) {
            return;
        }

        targetField.set(current, sourceText);
        overwritten(context, targetField.getName(), currentText, sourceText);
    }

    private List<MultilingualContentDTO> mergeMultilingual(List<MultilingualContentDTO> source,
                                                           List<MultilingualContentDTO> current,
                                                           String fieldName,
                                                           MergeContext context) {
        var merged = new ArrayList<>(current);

        source.forEach(sourceContent -> {
            if (Objects.isNull(sourceContent) || isEmpty(sourceContent.getContent())) {
                return;
            }

            var match = merged.stream()
                .filter(currentContent -> sameLanguage(currentContent, sourceContent))
                .findFirst();

            if (match.isEmpty()) {
                merged.add(new MultilingualContentDTO(sourceContent));
                return;
            }

            var currentContent = match.get();

            if (isEmpty(currentContent.getContent())) {
                currentContent.setContent(sourceContent.getContent());
                return;
            }

            if (sourceContent.getContent().trim().length() >
                currentContent.getContent().trim().length()) {
                overwritten(context, fieldName + "[" + languageOf(currentContent) + "]",
                    currentContent.getContent(), sourceContent.getContent());
                currentContent.setContent(sourceContent.getContent());
            }
        });

        return merged;
    }

    private void overwritten(MergeContext context, String fieldName, String currentText,
                             String sourceText) {
        migrationLog.valueDropped(context.source(), context.entity(), context.key(), RULE,
            fieldName + " overwritten: '" + shorten(currentText) + "' -> '" +
                shorten(sourceText) + "'");
    }

    private boolean sameLanguage(MultilingualContentDTO first, MultilingualContentDTO second) {
        if (Objects.isNull(first) || Objects.isNull(second)) {
            return false;
        }

        if (Objects.nonNull(first.getLanguageTag()) && Objects.nonNull(second.getLanguageTag())) {
            return first.getLanguageTag().equalsIgnoreCase(second.getLanguageTag());
        }

        return Objects.nonNull(first.getLanguageTagId()) &&
            Objects.equals(first.getLanguageTagId(), second.getLanguageTagId());
    }

    private String languageOf(MultilingualContentDTO content) {
        return Objects.isNull(content.getLanguageTag())
            ? String.valueOf(content.getLanguageTagId())
            : content.getLanguageTag();
    }

    private boolean isMultilingual(Object value) {
        return value instanceof List<?> list && !list.isEmpty() &&
            list.getFirst() instanceof MultilingualContentDTO;
    }

    @SuppressWarnings("unchecked")
    private List<MultilingualContentDTO> asMultilingual(Object value) {
        return (List<MultilingualContentDTO>) value;
    }

    private boolean isEmpty(Object value) {
        if (Objects.isNull(value)) {
            return true;
        }

        if (value instanceof String text) {
            return text.isBlank();
        }

        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        return value instanceof Map<?, ?> map && map.isEmpty();
    }

    private String shorten(String text) {
        return text.length() <= 80 ? text : text.substring(0, 77) + "...";
    }

    private List<Field> fieldsOf(Class<?> type) {
        return fieldCache.computeIfAbsent(type, key -> {
            var fields = new ArrayList<Field>();

            for (var current = key; Objects.nonNull(current) && !Object.class.equals(current);
                 current = current.getSuperclass()) {
                for (var field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                        continue;
                    }

                    field.setAccessible(true);
                    fields.add(field);
                }
            }

            return List.copyOf(fields);
        });
    }

    private Field findField(Class<?> type, String name) {
        return fieldsOf(type).stream()
            .filter(field -> field.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
