import React, { memo } from 'react';
import { Button } from "@wrappers/index";

const CreateButton = memo<any>((props) => {
    return (
        <Button
            elevated
            text="Create"
            rightIcon="item-add-artefact"
            {...props}
        />
    )
});

export default CreateButton;
