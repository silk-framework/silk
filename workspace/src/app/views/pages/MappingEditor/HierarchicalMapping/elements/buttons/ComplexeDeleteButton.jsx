import { Button } from "@eccenca/gui-elements";
import React from "react";

const ComplexDeleteButton = ({ onDelete }) => {
    return (
        <Button
            data-test-id={"mapping-rule-complex-delete-btn"}
            raised
            iconName="delete"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-delete"
            onClick={onDelete}
            tooltip="Reset to default pattern"
        />
    );
};

export default ComplexDeleteButton;
