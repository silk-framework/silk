import { Button } from "@eccenca/gui-elements";
import React from "react";

const CopyButton = ({ onCopy }) => {
    return (
        <Button
            data-test-id={"mapping-rule-copy-btn"}
            className="ecc-silk-mapping__rulesviewer__actionrow-copy"
            onClick={onCopy}
        >
            Copy
        </Button>
    );
};

export default CopyButton;
