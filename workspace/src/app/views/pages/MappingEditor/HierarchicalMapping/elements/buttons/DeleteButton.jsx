import { Button } from "@eccenca/gui-elements";
import React from "react";

const DeleteButton = ({ onDelete }) => {
    return (
        <Button
            disruptive
            data-test-id={"mapping-rule-delete-btn"}
            className="ecc-silk-mapping__rulesviewer__actionrow-remove"
            raised
            onClick={onDelete}
        >
            Remove
        </Button>
    );
};

export default DeleteButton;
