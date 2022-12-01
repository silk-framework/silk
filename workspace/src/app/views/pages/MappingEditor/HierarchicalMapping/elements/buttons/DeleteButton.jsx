import { DisruptiveButton } from "@eccenca/gui-elements/src/legacy-replacements";
import React from "react";

const DeleteButton = ({ onDelete }) => {
    return (
        <DisruptiveButton className="ecc-silk-mapping__rulesviewer__actionrow-remove" raised onClick={onDelete}>
            Remove
        </DisruptiveButton>
    );
};

export default DeleteButton;
