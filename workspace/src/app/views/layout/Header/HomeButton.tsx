import React, { memo } from 'react';
import Icon from "@wrappers/blueprint/icon";
import { IconNames, Intent } from "@wrappers/blueprint/constants";

const HomeButton = memo(() => {
    return (
        <div className='home-button__container'>
            <Icon icon={IconNames.HOME} intent={Intent.PRIMARY} iconSize={26} style={{margin: '0 15px', color: 'black'}}/>
        </div>
    )
});

export default HomeButton;
