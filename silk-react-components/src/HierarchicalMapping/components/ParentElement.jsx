import _ from 'lodash';
import { ThingName } from './ThingName';
import React from 'react';

export const ParentElement = ({parent, ...otherProps}) =>
    (_.get(parent, 'type') ? (
        <ThingName id={parent.type} {...otherProps} />
    ) : (
        <span {...otherProps}>parent element</span>
    ));
