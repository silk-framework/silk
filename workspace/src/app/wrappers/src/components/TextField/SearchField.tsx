import React from 'react';
import Icon from '../Icon/Icon';
import TextField from './TextField';

function SearchField({
    className='',
    ...otherProps
}: any) {
    return (
        <TextField
            className={'ecc-textfield--searchinput ' + className}
            dir={'auto'}
            placeholder={'Enter search term'}
            aria-label={'Enter search term'}
            {...otherProps}
            type={'search'}
            leftIcon={'operation-search'}
            round={true}
        />
    );
};

export default SearchField;
