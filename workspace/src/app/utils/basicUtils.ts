/** Returns an array with values 0 ... (nrItems - 1) */
export const rangeArray = (nrItems: number): number[] => {
    const indexes = Array(nrItems).keys();
    // @ts-ignore
    return [...indexes];
};

/** Partitions an array into matching elements and non-matching elements for a specific predicate. */
export const partitionArray = <T>(inputArray: T[], predicate: (T) => boolean): { matches: T[]; nonMatches: T[] } => {
    const matches: T[] = [];
    const nonMatches: T[] = [];
    inputArray.forEach((elem: T) => (predicate(elem) ? matches.push(elem) : nonMatches.push(elem)));
    return { matches, nonMatches };
};

/** Sorts an array based on a derived string value lexically. */
export const sortLexically = <T>(
    inputArray: T[],
    by: (elem: T) => string,
    asc: boolean = true,
    caseInsensitive: boolean = true,
    inPlace: boolean = true
) => {
    const transform = (elem: T) => (caseInsensitive ? by(elem).toLowerCase() : by(elem));
    const comparison = asc ? (a, b) => (a < b ? -1 : 1) : (a, b) => (a > b ? 1 : -1);
    return (inPlace ? inputArray : [...inputArray]).sort((a, b) => comparison(transform(a), transform(b)));
};

/** Sets the new value based on the picker function. */
export const setConditionalMap = <KeyType, ValueType>(
    map: Map<KeyType, ValueType>,
    key: KeyType,
    newValue: ValueType,
    valuePicker: (newValue: ValueType, oldValue?: ValueType) => ValueType
): ValueType => {
    const valueToSet = valuePicker(newValue, map.get(key));
    map.set(key, valueToSet);
    return valueToSet;
};

/** Picks the larger number. */
export const maxNumberValuePicker = (newValue: number, oldValue?: number): number => {
    if (oldValue) {
        return newValue > oldValue ? newValue : oldValue;
    } else {
        return newValue;
    }
};
