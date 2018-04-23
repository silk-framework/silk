import React from 'react';
import classNames from 'classnames';
import ReactMDLSpinner from 'react-mdl/lib/Spinner';
import PerformanceMixin from './../../mixins/PerformanceMixin';

const Spinner = React.createClass({
    mixins: [PerformanceMixin],

    // define property types
    propTypes: {
        appearGlobal: React.PropTypes.bool,
        appearInline: React.PropTypes.bool,
        appearLocal: React.PropTypes.bool,
        className: React.PropTypes.string,
    },
    getDefaultProps() {
        return {
            appearGlobal: true,
            appearInline: false,
            appearLocal: false,
        };
    },

    // template rendering
    render() {
        const {
            className,
            appearGlobal,
            appearInline,
            appearLocal,
            ...otherProps
        } = this.props;
        const classes = classNames(
            {
                'mdl-spinner--global':
                    appearGlobal === true &&
                    appearInline === false &&
                    appearLocal === false,
                'mdl-spinner--inline': appearInline === true,
                'mdl-spinner--local': appearLocal === true,
            },
            className
        );
        return (
            <ReactMDLSpinner singleColor className={classes} {...otherProps} />
        );
    },
});

export default Spinner;
