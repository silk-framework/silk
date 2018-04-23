import React from 'react';
import ReactMDLSwitch from 'react-mdl/lib/Switch';
import extendedOnChangeBoolean from '../../utils/extendedOnChangeBoolean';

const Switch = props => {
    const {
        label,
        children,
        checked,
        ripple = false,
        onChange,
        ...otherProperties
    } = props;

    let checkboxlabel = label || false;
    if (!checkboxlabel && children) {
        checkboxlabel = children;
    }

    return (
        <ReactMDLSwitch
            checked={!!checked}
            ripple={ripple}
            onChange={extendedOnChangeBoolean.bind(null, onChange)}
            {...otherProperties}>
            {checkboxlabel}
        </ReactMDLSwitch>
    );
};

Switch.propTypes = {
    checked: React.PropTypes.bool.isRequired,
    onChange: React.PropTypes.func.isRequired,
};

export default Switch;
