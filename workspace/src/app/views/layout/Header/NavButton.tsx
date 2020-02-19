import React, { memo } from 'react';
import Icon from "@wrappers/icon";
import { IconNames, Intent } from "@wrappers/constants";

const NavButton = memo(() => {
    return (
        <div className='nav-button__container'>
            <Icon icon={IconNames.MENU} intent={Intent.PRIMARY} iconSize={24} style={{color: 'black'}}/>
        </div>
    )
});

export default NavButton;
