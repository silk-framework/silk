import { Button } from "@eccenca/gui-elements/src/legacy-replacements";
import React from "react";

const CopyButton = ({ onCopy }) => {
    return (
        <Button className="ecc-silk-mapping__rulesviewer__actionrow-copy" raised onClick={onCopy}>
            Copy
        </Button>
    );
};

export default CopyButton;
