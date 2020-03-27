import React from 'react';
import Button from '../Button/Button';
import Icon from './Icon';

function IconButton({
    className='',
    name = 'undefined',
    text,
    large,
    ...restProps
}: any) {

    return (
        <Button
            {...restProps}
            icon={<Icon name={name} large={large} description={text} />}
            className={'ecc-button--icon ' + className}
            minimal
        />
    );
};

export default IconButton;
