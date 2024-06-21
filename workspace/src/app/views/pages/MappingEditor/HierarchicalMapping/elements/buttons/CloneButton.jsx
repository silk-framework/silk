import React from "react";
import { Button } from "@eccenca/gui-elements/src/legacy-replacements";

const CloneButton = ({ onClone }) => {
    return (
        <Button
            data-test-id={"mapping-rule-clone-btn"}
            className="ecc-silk-mapping__rulesviewer__actionrow-clone"
            raised
            onClick={onClone}
        >
            Clone
        </Button>
    );
};

export default CloneButton;
