import { URIInfo } from './URIInfo';
import React from 'react';

export const ThingName = ({id, ...otherProps}) => (
    <URIInfo uri={id} {...otherProps} field="label"/>
);
