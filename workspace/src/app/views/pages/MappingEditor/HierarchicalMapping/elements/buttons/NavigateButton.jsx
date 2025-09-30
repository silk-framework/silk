import { IconButton } from "@eccenca/gui-elements";
import React from "react";

const NavigateButton = ({ onClick, id }) => {
    return (
        <IconButton data-test-id={`button-${id}`} className={`silk${id}`} name="navigation-next" onClick={onClick} />
    );
};

export default NavigateButton;
