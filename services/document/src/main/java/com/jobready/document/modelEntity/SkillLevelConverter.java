package com.jobready.document.modelEntity;

import com.jobready.document.generated.modelDto.SkillLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link SkillLevel} using its wire value (e.g. {@code beginner}) rather than the Java
 * enum name ({@code BEGINNER}). This keeps the persisted value identical to the OpenAPI contract
 * and the DB {@code CHECK (level IN (...))} constraint — the generated enum is the single source
 * of truth for the allowed values.
 */
@Converter(autoApply = true)
public class SkillLevelConverter implements AttributeConverter<SkillLevel, String> {

    @Override
    public String convertToDatabaseColumn(SkillLevel level) {
        return level == null ? null : level.getValue();
    }

    @Override
    public SkillLevel convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : SkillLevel.fromValue(dbValue);
    }
}
