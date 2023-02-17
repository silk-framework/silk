/** Converts a value type string to a more generic type, human-readable. */
const valueTypeLabel = (valueType: string): string => {
    switch (valueType) {
        case "StringValueType":
            return "String";
        case "IntegerValueType":
            return "Integer";
        case "IntValueType":
            return "Int";
        case "LongValueType":
            return "Long";
        case "FloatValueType":
            return "Float";
        case "DoubleValueType":
            return "Double";
        case "DecimalValueType":
            return "Decimal";
        case "BooleanValueType":
            return "Boolean";
        case "UriValueType":
            return "URI";
        case "AnyDateTimeValueType":
            return "DateTime (all types)";
        case "DateTimeValueType":
            return "DateTime";
        case "AnyDateValueType":
            return "Date (all types)";
        case "DateValueType":
            return "Date";
        case "TimeValueType":
            return "Time";
        case "YearValueType":
            return "Year";
        case "YeahMonthValueType":
            return "YearMonth";
        case "MonthDayValueType":
            return "MonthDay";
        case "DayValueType":
            return "Day";
        case "MonthValueType":
            return "Month";
        case "DurationValueType":
            return "Duration";
        case "WktValueType":
            return "Geometry (WKT literal)";
        default:
            return valueType;
    }
};

const utils = {
    valueTypeLabel,
};

export default utils;
