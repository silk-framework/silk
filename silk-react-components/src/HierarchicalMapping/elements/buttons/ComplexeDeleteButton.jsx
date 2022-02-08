import React from 'react';
import { Button } from "gui-elements/legacy-replacements";

const ComplexDeleteButton = ({ onDelete }) => {
    return (
        <Button
            raised
            iconName="delete"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-delete"
            onClick={onDelete}
            tooltip="Reset to default pattern"
        />
    )
};

export default ComplexDeleteButton;
