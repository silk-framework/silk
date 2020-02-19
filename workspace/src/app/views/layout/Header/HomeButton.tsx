import React, { memo } from 'react';
import Icon from "@wrappers/bluprint/icon";
import { IconNames, Intent } from "@wrappers/bluprint/constants";

const HomeButton = memo(() => {
    return (
        <div className='home-button__container'>
            <Icon icon={IconNames.HOME} intent={Intent.PRIMARY} iconSize={26} style={{margin: '0 15px', color: 'black'}}/>
        </div>
    )
});

export default HomeButton;
