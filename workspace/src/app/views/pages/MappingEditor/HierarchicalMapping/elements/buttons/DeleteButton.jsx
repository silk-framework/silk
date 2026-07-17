import { Button } from "@eccenca/gui-elements";
import React from "react";

const DeleteButton = ({ onDelete }) => {
    return (
        <Button disruptive data-test-id={"mapping-rule-delete-btn"} onClick={onDelete}>
            Remove
        </Button>
    );
};

export default DeleteButton;
