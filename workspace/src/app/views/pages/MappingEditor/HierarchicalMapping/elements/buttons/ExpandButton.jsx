import { IconButton } from "@eccenca/gui-elements";
import React from "react";

const ExpandButton = ({ onToggle, id, expanded }) => {
    return (
        <IconButton
            data-test-id={`button-${id}`}
            className={`silk${id}`}
            name={expanded ? "toggler-rowcollapse" : "toggler-rowexpand"}
            onClick={onToggle}
        />
    );
};

export default ExpandButton;
