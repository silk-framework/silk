import React from 'react';
import { DisruptiveButton } from "gui-elements/legacy-replacements";

const DeleteButton = ({ onDelete }) => {
    return (
        <DisruptiveButton
            className="ecc-silk-mapping__rulesviewer__actionrow-remove"
            raised
            onClick={onDelete}
        >
            Remove
        </DisruptiveButton>
    )
};

export default DeleteButton;
