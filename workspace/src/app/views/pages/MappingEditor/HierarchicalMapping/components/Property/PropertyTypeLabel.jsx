import { PropertyTypeInfo } from './PropertyTypeInfo';
import React from 'react';

export const PropertyTypeLabel = ({name, appendedText}) => (
    <PropertyTypeInfo name={name} option="label" appendedText={appendedText}/>
);
