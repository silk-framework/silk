import React from 'react';
import { Button } from '@eccenca/gui-elements';

const EditButton = ({ onEdit }) => {
    return (
        <Button
            className="ecc-silk-mapping__rulesviewer__actionrow-edit"
            raised
            onClick={onEdit}
        >
            Edit
        </Button>
    )
};

export default EditButton;
