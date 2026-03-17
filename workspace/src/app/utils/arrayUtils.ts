const arrayUtils = {
    /** Returns a sorted array with duplicate values removed. */
    distinctSort: <T>(arr: T[]): T[] => [...new Set(arr)].sort(),
};

export default arrayUtils;
