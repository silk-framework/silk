import React from "react";

function Divider({ addSpacing = "none" }: any) {
    return <hr className={"ecc-separation__divider-horizontal " + "ecc-separation__spacing--" + addSpacing} />;
}

export default Divider;
