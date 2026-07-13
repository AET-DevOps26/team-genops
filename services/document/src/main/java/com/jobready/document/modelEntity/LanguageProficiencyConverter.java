package com.jobready.document.modelEntity;

import com.jobready.document.generated.modelDto.LanguageProficiency;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link LanguageProficiency} using its wire value (e.g. {@code fluent}) rather than the
 * Java enum name ({@code FLUENT}). This keeps the persisted value identical to the OpenAPI
 * contract and the DB {@code CHECK (proficiency IN (...))} constraint — the generated enum is the
 * single source of truth for the allowed values.
 */
@Converter(autoApply = true)
public class LanguageProficiencyConverter implements AttributeConverter<LanguageProficiency, String> {

    @Override
    public String convertToDatabaseColumn(LanguageProficiency proficiency) {
        return proficiency == null ? null : proficiency.getValue();
    }

    @Override
    public LanguageProficiency convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : LanguageProficiency.fromValue(dbValue);
    }
}
