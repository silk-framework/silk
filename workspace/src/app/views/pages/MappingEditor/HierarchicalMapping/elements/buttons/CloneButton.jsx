import { Button } from "@eccenca/gui-elements/src/legacy-replacements";
import React from "react";

const CloneButton = ({ onClone }) => {
    return (
        <Button className="ecc-silk-mapping__rulesviewer__actionrow-clone" raised onClick={onClone}>
            Clone
        </Button>
    );
};

export default CloneButton;
