import React from 'react';
import classNames from 'classnames';
import ReactMDLProgressBar from 'react-mdl/lib/ProgressBar';

const Progressbar = React.createClass({
    // define property types
    propTypes: {
        appearGlobal: React.PropTypes.bool,
        appearLocal: React.PropTypes.bool,
        className: React.PropTypes.string,
    },
    getDefaultProps() {
        return {
            appearGlobal: false,
            appearLocal: false,
        };
    },

    // template rendering
    render() {
        const {
            className,
            appearGlobal,
            appearLocal,
            ...otherProps
        } = this.props;
        const classes = classNames(
            {
                'mdl-progress--global': appearGlobal === true,
                'mdl-progress--local': appearLocal === true,
            },
            className
        );
        return <ReactMDLProgressBar className={classes} {...otherProps} />;
    },
});

export default Progressbar;
