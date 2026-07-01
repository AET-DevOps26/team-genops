package com.jobready.application.modelEntity;

import com.jobready.application.generated.modelDto.ApplicationStage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link ApplicationStage} using its wire value (e.g. {@code follow_up}) rather than the
 * Java enum name ({@code FOLLOW_UP}). This keeps the persisted value identical to the OpenAPI
 * contract and the DB {@code CHECK (stage IN (...))} constraint — the generated enum is the single
 * source of truth for the allowed values.
 */
@Converter(autoApply = true)
public class StageConverter implements AttributeConverter<ApplicationStage, String> {

    @Override
    public String convertToDatabaseColumn(ApplicationStage stage) {
        return stage == null ? null : stage.getValue();
    }

    @Override
    public ApplicationStage convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : ApplicationStage.fromValue(dbValue);
    }
}
