import React from 'react';
import { DisruptiveButton } from '@eccenca/gui-elements';

const DeleteButton = ({ onDelete }) => {
    return (
        <DisruptiveButton
            className="ecc-silk-mapping__rulesviewer__actionrow-remove"
            raised
            onClick={onDelete}
        >
            Remove
        </DisruptiveButton>
    )
};

export default DeleteButton;
