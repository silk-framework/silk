import React from 'react';
import {
    Radio,
    RadioGroup,
} from '@eccenca/gui-elements';
import { ParentElement } from '../../../../Components/ParentElement';

const ObjectEntityRelation = ({ isBackwardProperty, parent }) => {
    return (
        <RadioGroup
            value={isBackwardProperty ? 'to' : 'from'}
            name=""
            disabled
        >
            <Radio
                value="from"
                label={
                    <div>
                        Connect from{' '}
                        <ParentElement parent={parent} />
                    </div>
                }
            />
            <Radio
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
