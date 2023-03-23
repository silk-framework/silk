import React from "react";

export const useFirstRender = () => {
    const hasRenderedBefore = React.useRef(false);
    React.useLayoutEffect(() => {
        if (!hasRenderedBefore.current) {
            hasRenderedBefore.current = true;
        }
    });
    return hasRenderedBefore.current;
};
