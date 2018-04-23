import React from 'react';
import Alert from './Alert';
import PerformanceMixin from '../../mixins/PerformanceMixin';

const Info = React.createClass({
    mixins: [PerformanceMixin],

    // define property types
    propTypes: {
        children: React.PropTypes.node.isRequired,
    },

    // template rendering
    render() {
        const {children, ...otherProps} = this.props;

        return (
            <Alert type="info" {...otherProps}>
                {children}
            </Alert>
        );
    },
});

export default Info;
