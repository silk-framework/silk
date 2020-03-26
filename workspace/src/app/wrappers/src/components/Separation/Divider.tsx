import React from 'react';

const Divider = ({
    addSpacing="none"
}: any) => {
    return (
        <hr
            className={
                'ecc-separation__divider-horizontal ' +
                'ecc-separation__spacing-horizontal--' + addSpacing
            }
        />
    );
};

export default Divider;
