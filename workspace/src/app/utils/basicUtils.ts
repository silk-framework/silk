/** Returns an array with values 0 ... (nrItems - 1) */
export const rangeArray = (nrItems: number): number[] => {
    const indexes = Array(nrItems).keys();
    // @ts-ignore
    return [...indexes];
};
