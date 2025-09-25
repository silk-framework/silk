import { Button } from "@eccenca/gui-elements";
import React from "react";

const ExpandButton = ({ onToggle, id, expanded }) => {
    return (
        <Button
            data-test-id={`button-${id}`}
            className={`silk${id}`}
            iconName={expanded ? "expand_less" : "expand_more"}
            onClick={onToggle}
        />
    );
};

export default ExpandButton;
