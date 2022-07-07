import React from 'react';
import { Button } from "@eccenca/gui-elements/src/legacy-replacements";

const ArrowBackButton = ({ onNavigate }) => {
    return (
        <Button
            iconName="arrow_back"
            tooltip="Navigate back to parent"
            onClick={onNavigate}
            data-button-id={'back'}
        />
    )
};

export default ArrowBackButton;
