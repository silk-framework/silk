import React from 'react';
import ReactMDLRadio from 'react-mdl/lib/Radio';

const Radio = props => {
    const {children, label, ripple, ...otherProps} = props;

    let radioLabel = false;

    if (label && !children) {
        radioLabel = label;
    }

    if (!label && children) {
        radioLabel = children;
    }

    if (label && children) {
        radioLabel = <div title={label}>{children}</div>;
    }

    if (__DEBUG__ && radioLabel === false) {
        console.warn(
            'Add label property or children content to <Radio /> element.'
        );
    }

    return (
        <ReactMDLRadio ripple={ripple || false} {...otherProps}>
            {radioLabel || <span>&nbsp;</span>}
        </ReactMDLRadio>
    );
};

export default Radio;
