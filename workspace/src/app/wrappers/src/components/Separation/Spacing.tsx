import React from "react";

function Spacing({ size = "medium", hasDivider = false, vertical = false }: any) {
    const direction = vertical ? "vertical" : "horizontal";
    return (
        <div
            className={
                "ecc-separation__spacing-" +
                direction +
                " ecc-separation__spacing--" +
                size +
                (hasDivider ? " ecc-separation__spacing--hasdivider" : "")
            }
        />
    );
}

export default Spacing;
