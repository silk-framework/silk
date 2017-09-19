import React from 'react';
import {Error} from 'ecc-gui-elements';

const ErrorView = React.createClass({
    propTypes: {
        title: React.PropTypes.string,
        detail: React.PropTypes.string,
        cause: React.PropTypes.object, // it may contain a list for errors with title and detail itself
    },
    componentDidMount() {

    },
    getInitialState() {
        return {
            errorExpanded: false,
        };
    },
    // template rendering
    render() {
        const errorClassName = this.state.errorExpanded
                ? ''
                : 'mdl-alert--narrowed';

        return <Error
            border
            className={errorClassName}
            handlerDismiss={() => {
                this.setState({
                    errorExpanded: !this.state.errorExpanded,
                });
            }}
            labelDismiss={
                this.state.errorExpanded ? 'Show less' : 'Show more'
            }
            iconDismiss={
                this.state.errorExpanded ? 'expand_less' : 'expand_more'
            }>
            <p>{this.props.title}</p>
            <p>{this.props.title}</p>

        </Error>;
    }
});

export default ExampleView;
