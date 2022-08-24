/** Converts a value type string to a more generic type, human-readable. */
const convertValueType = (valueType: string): string => {
    switch (valueType) {
        case "StringValueType":
            return "string";
        case "IntegerValueType":
        case "IntValueType":
        case "LongValueType":
        case "FloatValueType":
        case "DoubleValueType":
        case "DecimalValueType":
            return "number";
        case "BooleanValueType":
            return "boolean";
        case "UriValueType":
            return "uri";
        case "AnyDateTimeValueType":
        case "DateTimeValueType":
            return "date/time";
        case "AnyDateValueType":
        case "DateValueType":
            return "date";
        case "TimeValueType":
            return "time";
        case "YearValueType":
            return "year";
        case "YeahMonthValueType":
            return "year and month";
        case "MonthDayValueType":
            return "month and day";
        case "DayValueType":
            return "day";
        case "MonthValueType":
            return "month";
        case "DurationValueType":
            return "duration";
        case "WktValueType":
            return "geo (wkt)";
        default:
            return valueType;
    }
};

const utils = {
    convertValueType,
};

export default utils;
