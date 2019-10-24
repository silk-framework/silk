import React from 'react';
import { Button } from '@eccenca/gui-elements';

const ComplexEditButton = ({ onClick, href, tooltip }) => {
    return (
        <Button
            raised
            iconName="edit"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
            onClick={onClick}
            href={href}
            tooltip={tooltip}
        />
    )
}

export default ComplexEditButton;
