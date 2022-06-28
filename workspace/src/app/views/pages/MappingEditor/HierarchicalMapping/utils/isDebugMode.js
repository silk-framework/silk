export const isDebugMode = (str) => {
    const isDebug = !!__DEBUG__;
    if (isDebug && str) {
        console.warn(str);
    }
    return isDebug;
};
