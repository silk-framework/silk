import React, { memo } from 'react';
import { Icon, Intent } from "@blueprintjs/core";
import { IconNames } from "@blueprintjs/icons";

const NavButton = memo(() => {
    return (
        <div className='nav-button__container'>
            <Icon icon={IconNames.MENU} intent={Intent.PRIMARY} />
        </div>
    )
});

export default NavButton;
