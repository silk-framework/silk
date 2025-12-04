import { IconButton } from "@eccenca/gui-elements";
import React from "react";

const ComplexDeleteButton = ({ onDelete }) => {
    return (
        <IconButton
            data-test-id={"mapping-rule-complex-delete-btn"}
            name="item-remove"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-delete"
            onClick={onDelete}
            text="Reset to default pattern"
        />
    );
};

export default ComplexDeleteButton;
