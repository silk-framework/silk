package org.silkframework.rule.plugins.transformer.date;

import java.time.temporal.ChronoUnit;

public enum DateUnit {

    milliseconds, seconds, day, month, year;

    /**
     * Returns the corresponding Java ChronoUnit.
     */
    public ChronoUnit toChronoUnit() {
        switch (this) {
            case milliseconds:
                return ChronoUnit.MILLIS;
            case seconds:
                return ChronoUnit.SECONDS;
            case day:
                return ChronoUnit.DAYS;
            case month:
                return ChronoUnit.MONTHS;
            case year:
                return ChronoUnit.YEARS;
        }
        throw new IllegalArgumentException("Unsupported unit: " + this);
    }
}
