import React from 'react';
import { Button } from '@gui-elements/legacy-replacements';

const ComplexEditButton = ({ onClick, href, tooltip }) => {
    let externalHref = {};
    if (href) {
        externalHref = {
            href
        }
    }
    return (
        <Button
            raised
            iconName="edit"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
            onClick={onClick}
            tooltip={tooltip}
            {...externalHref}
        />
    )
};

export default ComplexEditButton;
