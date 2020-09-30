import React from 'react';
import { Button } from '@eccenca/gui-elements';

const NavigateButton = ({ onClick, id }) => {
    return (
        <Button
            data-test-id={`button-${id}`}
            className={`silk${id}`}
            iconName={'arrow_nextpage'}
            onClick={onClick}
        />
    )
};

export default NavigateButton;
