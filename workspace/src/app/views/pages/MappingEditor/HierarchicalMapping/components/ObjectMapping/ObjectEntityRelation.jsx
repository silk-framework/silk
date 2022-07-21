import React from 'react';
import {
    Radio,
    RadioGroup,
} from 'gui-elements-deprecated';
import { ParentElement } from '../ParentElement';

const ObjectEntityRelation = ({ isBackwardProperty, parent }) => {
    return (
        <RadioGroup
            value={isBackwardProperty ? 'to' : 'from'}
            name=""
            disabled
        >
            <Radio
                name="from"
                checked={!isBackwardProperty}
                value="from"
                label={
                    <div>
                        Connect from{' '}
                        <ParentElement parent={parent} />
                    </div>
                }
            />
            <Radio
                name="to"
                checked={isBackwardProperty}
                value="to"
                label={
                    <div>
                        Connect to{' '}
                        <ParentElement parent={parent} />
                    </div>
                }
            />
        </RadioGroup>
    )
};

export default ObjectEntityRelation;
