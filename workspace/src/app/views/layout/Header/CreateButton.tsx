import React, { memo } from 'react';
import { Icon } from "@wrappers/index";

const CreateButton = memo<any>((props) => {
    return (
        <Icon
            name="application-create-button"
            description="TODO: Create Button"
            large
            {...props}
        />
    )
});

export default CreateButton;
