import { Button } from "@eccenca/gui-elements";
import React from "react";

const EditButton = ({ onEdit }) => {
    return (
        <Button
            data-test-id={"mapping-rule-edit-btn"}
            className="ecc-silk-mapping__rulesviewer__actionrow-edit"
            raised
            onClick={onEdit}
        >
            Edit
        </Button>
    );
};

export default EditButton;
