import React from 'react';

const Spacing = ({
    size="medium"
}: any) => {
    return (
        <div
            className={
                'ecc-separation__spacing-horizontal ' +
                'ecc-separation__spacing-horizontal--' + size
            }
        />
    );
};

export default Spacing;
