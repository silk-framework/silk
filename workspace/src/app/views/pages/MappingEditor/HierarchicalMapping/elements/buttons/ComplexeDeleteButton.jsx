import { Button } from "@eccenca/gui-elements/src/legacy-replacements";
import React from "react";

const ComplexDeleteButton = ({ onDelete }) => {
    return (
        <Button
            raised
            iconName="delete"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-delete"
            onClick={onDelete}
            tooltip="Reset to default pattern"
        />
    );
};

export default ComplexDeleteButton;
