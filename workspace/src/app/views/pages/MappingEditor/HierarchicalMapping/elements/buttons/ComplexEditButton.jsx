import { Button } from "@eccenca/gui-elements";
import React from "react";

/** Edit button for complex rule formula. */
const ComplexEditButton = ({ onClick, href, tooltip }) => {
    let externalHref = {};
    if (href) {
        externalHref = {
            href,
        };
    }
    return (
        <Button
            data-test-id={"complex-rule-edit-button"}
            raised
            iconName="edit"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
            onClick={onClick}
            tooltip={tooltip}
            {...externalHref}
        />
    );
};

export default ComplexEditButton;
