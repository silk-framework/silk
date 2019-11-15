import React from 'react';

export const isClassComponent = (component) => {
    return (
        typeof component === 'function' &&
        !!component.prototype.isReactComponent
    )
};

export const isFunctionComponent = (component) => {
    return (
        typeof component === 'function' &&
        String(component).includes('return React.createElement')
    );
};

export const isReactComponent = (component) => {
    return (
        isClassComponent(component) ||
        isFunctionComponent(component)
    );
};

export const isElement = (element) => {
    return React.isValidElement(element);
};

export const isDOMTypeElement = (element) => {
    return isElement(element) && typeof element.type === 'string';
};

export const isCompositeTypeElement = (element) => {
    return isElement(element) && typeof element.type === 'function';
};

