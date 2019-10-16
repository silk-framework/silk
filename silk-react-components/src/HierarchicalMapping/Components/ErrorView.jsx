import React from 'react';
import PropTypes from 'prop-types';

import { Error } from '@eccenca/gui-elements';
import _ from 'lodash';

export const ErrorCause = ({ errorCause }) => (
    <ul className="ecc-hierarchical-mapping-error-list">
        {_.map(errorCause, ({ title, detail, cause }) => {
            let renderedCause = false;

            if (_.isArray(cause)) {
                renderedCause = <ErrorCause errorCause={cause} />;
            }

            return (
                <li>
                    <p>{title}</p>
                    <p>{detail}</p>
                    {renderedCause}
                </li>
            );
        })}
    </ul>
);

export const ErrorIssue = ({ errorCause }) => (
    <ul className="ecc-hierarchical-mapping-error-list">
        {_.map(errorCause, ({ message }) => (
            <li>
                <p>{message}</p>
            </li>
        ))}
    </ul>
);

export class ErrorView extends React.Component {
    static propTypes = {
        title: PropTypes.string,
        detail: PropTypes.string,
        cause: PropTypes.array, // it may contain a list for errors with title and detail itself
        issues: PropTypes.array, // it may contain a list for errors with title and detail itself, too
    };

    state = {
        errorExpanded: false,
    };

    toggleExpansion() {
        this.setState({
            errorExpanded: !this.state.errorExpanded,
        });
    }
    // template rendering
    render() {
        const errorClassName = this.state.errorExpanded
            ? ''
            : 'mdl-alert--narrowed';

        let causes = false;
        let issues = false;

        if (this.state.errorExpanded && _.isArray(this.props.cause)) {
            causes = <ErrorCause errorCause={this.props.cause} />;
        }

        if (this.state.errorExpanded && _.isArray(this.props.issues)) {
            issues = <ErrorIssue errorCause={this.props.issues} />;
        }

        const detail =
            this.props.title !== this.props.detail ? (
                <p>{this.props.detail}</p>
            ) : (
                false
            );

        return (
            <Error
                border
                className={errorClassName}
                handlerDismiss={this.toggleExpansion}
                labelDismiss={
                    this.state.errorExpanded ? 'Show less' : 'Show more'
                }
                iconDismiss={
                    this.state.errorExpanded ? 'expand_less' : 'expand_more'
                }
            >
                <strong>{this.props.title}</strong>
                {detail}
                {causes}
                {issues}
            </Error>
        );
    }
}

export default ErrorView;
