import _ from 'lodash';
import { ThingName } from './ThingName';
import { ParentElement } from './ParentElement';
import React from 'react';

export const ParentStructure = ({parent, ...otherProps}) =>
    (_.get(parent, 'property') ? (
        <ThingName id={parent.property} {...otherProps} />
    ) : (
        <ParentElement parent={parent} {...otherProps} />
    ));
