import React from 'react';
import { Button } from '@gui-elements/legacy-replacements';

const CopyButton = ({ onCopy }) => {
    return (
        <Button
            className="ecc-silk-mapping__rulesviewer__actionrow-copy"
            raised
            onClick={onCopy}
        >
            Copy
        </Button>
    )
};

export default CopyButton;
