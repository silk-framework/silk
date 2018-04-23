import React from 'react';
import classNames from 'classnames';
import Button from '../../elements/Button/Button';

const Alert = React.createClass({
    // define property types
    propTypes: {
        children: React.PropTypes.node.isRequired,
        className: React.PropTypes.string,
        handlerDismiss: React.PropTypes.func,
        labelDismiss: React.PropTypes.string,
        iconDismiss: React.PropTypes.string,
        type: React.PropTypes.string,
        border: React.PropTypes.bool,
        vertSpacing: React.PropTypes.bool,
    },

    // template rendering
    render() {
        const {
            className,
            border,
            handlerDismiss,
            labelDismiss,
            iconDismiss,
            type,
            vertSpacing,
            children,
            ...otherProps
        } = this.props;

        const classes = classNames(
            'mdl-alert',
            {
                'mdl-alert--info': type === 'info',
                'mdl-alert--success': type === 'success',
                'mdl-alert--warning': type === 'warning',
                'mdl-alert--danger': type === 'error',
                'mdl-alert--border': border,
                'mdl-alert--spacing': vertSpacing,
                'mdl-alert--dismissable': typeof handlerDismiss !== 'undefined',
            },
            className
        );

        // TODO: add onclick event to remove alert
        let dismiss = false;
        if (handlerDismiss) {
            dismiss = (
                <div className="mdl-alert__dismiss">
                    <Button
                        type="button"
                        iconName={iconDismiss || 'close'}
                        tooltip={labelDismiss || 'Hide'}
                        onClick={handlerDismiss}
                    />
                </div>
            );
        }

        return (
            <div className={classes} {...otherProps}>
                <div className="mdl-alert__content">{children}</div>
                {dismiss}
            </div>
        );
    },
});

export default Alert;
