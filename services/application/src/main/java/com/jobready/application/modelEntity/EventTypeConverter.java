package com.jobready.application.modelEntity;

import com.jobready.application.generated.modelDto.ApplicationEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link ApplicationEventType} using its wire value (e.g. {@code stage_change}) rather
 * than the Java enum name, mirroring {@link StageConverter} — the generated enum stays the single
 * source of truth for the allowed values.
 */
@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<ApplicationEventType, String> {

    @Override
    public String convertToDatabaseColumn(ApplicationEventType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public ApplicationEventType convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : ApplicationEventType.fromValue(dbValue);
    }
}
