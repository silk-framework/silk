/** Returns an array with values 0 ... (nrItems - 1) */
export const rangeArray = (nrItems: number): number[] => {
    const indexes = Array(nrItems).keys();
    // @ts-ignore
    return [...indexes];
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
