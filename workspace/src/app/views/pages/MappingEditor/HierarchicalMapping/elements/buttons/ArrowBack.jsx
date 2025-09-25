import { Button } from "@eccenca/gui-elements";
import React from "react";

const ArrowBackButton = ({ onNavigate }) => {
    return (
        <Button iconName="arrow_back" tooltip="Navigate back to parent" onClick={onNavigate} data-button-id={"back"} />
    );
};

export default ArrowBackButton;
