import React from 'react';
import {Error} from 'ecc-gui-elements';
import _ from 'lodash';

const ErrorCause = ({errorCause}) => {

    return <ul className="ecc-hierarchical-mapping-error-list">{
        _.map(errorCause, ({title, detail, cause}) => {

            let renderedCause = false;

            if(_.isArray(cause)){
                renderedCause = <ErrorCause errorCause={cause}/>;
            }

            return (
                <li>
                    <p>{title}</p>
                    <p>{detail}</p>
                    {renderedCause}
                </li>
            );
        })
    }</ul>;
};

const ErrorView = React.createClass({
    propTypes: {
        title: React.PropTypes.string,
        detail: React.PropTypes.string,
        cause: React.PropTypes.object, // it may contain a list for errors with title and detail itself
        issues: React.PropTypes.object // it may contain a list for errors with title and detail itself, too
    },
    componentDidMount() {

    },
    getInitialState() {
        return {
            errorExpanded: false,
        };
    },
    toggleExpansion(){
        this.setState({
            errorExpanded: !this.state.errorExpanded,
        });
    },
    // template rendering
    render() {

        const errorClassName = this.state.errorExpanded
            ? ''
            : 'mdl-alert--narrowed';

        let causes = false;
        let issues = false;

        if(this.state.errorExpanded && _.isArray(this.props.cause)) {
            causes = <ErrorCause errorCause={this.props.cause} />
        }

        if(this.state.errorExpanded && _.isArray(this.props.issues)) {
            issues = <ErrorCause errorCause={this.props.issues} />
        }

        return <Error
            border
            className={errorClassName}
            handlerDismiss={this.toggleExpansion}
            labelDismiss={
                this.state.errorExpanded ? 'Show less' : 'Show more'
            }
            iconDismiss={
                this.state.errorExpanded ? 'expand_less' : 'expand_more'
            }>
            <strong>{this.props.title}</strong>
            <p>{this.props.detail}</p>
            {causes}
            {issues}
        </Error>;
    }
});

export default ErrorView;
