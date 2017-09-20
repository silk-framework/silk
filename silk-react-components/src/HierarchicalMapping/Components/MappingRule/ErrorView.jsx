import React from 'react';
import {Error} from 'ecc-gui-elements';
import _ from 'lodash';

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
    recursiveRender(array) {
        return <ul className="ecc-hierarchical-mapping-error-list">{_.map(array, ({title, detail, cause}) => {
            return <li><p>{title}</p><p>{detail}</p><p>{this.recursiveRender(cause)}</p></li>
        })}</ul>;
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
            <p>{this.props.detail}</p>
            <ul className="ecc-hierarchical-mapping-error-list">{this.recursiveRender(this.props.cause)}</ul>

        </Error>;
    }
});

export default ErrorView;
