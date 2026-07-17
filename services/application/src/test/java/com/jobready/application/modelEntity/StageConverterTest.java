package com.jobready.application.modelEntity;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobready.application.generated.modelDto.ApplicationStage;
import org.junit.jupiter.api.Test;

/**
 * Guards the stage enum's consistency across the OpenAPI / Java / DB layers: persistence must use
 * the lowercase snake_case wire value (e.g. {@code follow_up}) — matching the OpenAPI contract and
 * the DB {@code CHECK (stage IN (...))} constraint — never the Java enum name ({@code FOLLOW_UP}).
 * The converter is exactly what maps the entity field to/from the stored column, so a round-trip
 * through it proves the persisted and re-read values stay on contract.
 */
class StageConverterTest {

    private final StageConverter converter = new StageConverter();

    @Test
    void persistsWireValue_notEnumName() {
        assertThat(converter.convertToDatabaseColumn(ApplicationStage.FOLLOW_UP))
                .isEqualTo("follow_up");
        assertThat(converter.convertToDatabaseColumn(ApplicationStage.APPLIED)).isEqualTo("applied");
    }

    @Test
    void readsWireValueBackToEnum() {
        assertThat(converter.convertToEntityAttribute("follow_up")).isEqualTo(ApplicationStage.FOLLOW_UP);
        assertThat(converter.convertToEntityAttribute("interview")).isEqualTo(ApplicationStage.INTERVIEW);
    }

    @Test
    void roundTripsEveryStage() {
        for (ApplicationStage stage : ApplicationStage.values()) {
            String stored = converter.convertToDatabaseColumn(stage);
            assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(stage);
        }
    }

    @Test
    void handlesNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
