/** Sort and distinct operation for string arrays in one. */
const distinctSort = (stringArray: string[]): string[] => {
    return [...new Set(stringArray)].sort((a, b) => a.localeCompare(b));
};

const utils = {
    distinctSort,
};

export default utils;
