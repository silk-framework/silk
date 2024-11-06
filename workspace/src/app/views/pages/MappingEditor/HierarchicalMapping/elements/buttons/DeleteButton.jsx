import React from "react";
import { DisruptiveButton } from "@eccenca/gui-elements/src/legacy-replacements";

const DeleteButton = ({ onDelete }) => {
    return (
        <DisruptiveButton
            data-test-id={"mapping-rule-delete-btn"}
            className="ecc-silk-mapping__rulesviewer__actionrow-remove"
            raised
            onClick={onDelete}
        >
            Remove
        </DisruptiveButton>
    );
};

export default DeleteButton;
