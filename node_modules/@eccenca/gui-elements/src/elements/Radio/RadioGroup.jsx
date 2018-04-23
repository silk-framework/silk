import _ from 'lodash';
import React from 'react';
import ReactMDLRadioGroup from 'react-mdl/lib/RadioGroup';

const extendedOnChange = (onChangeFn, event) => {
    if (_.isFunction(onChangeFn)) {
        onChangeFn({
            event,
            name: event.target.name,
            value: event.target.value,
            rawValue: event.target.value,
        });
    }
};

const RadioGroup = props => {
    const {onChange, container, childContainer, ...otherProps} = props;

    return (
        <div className="mdl-radio-group">
            <ReactMDLRadioGroup
                onChange={extendedOnChange.bind(null, onChange)}
                container={!childContainer && !container ? 'ul' : container}
                childContainer={
                    !childContainer && !container ? 'li' : childContainer
                }
                {...otherProps}
            />
        </div>
    );
};

export default RadioGroup;
