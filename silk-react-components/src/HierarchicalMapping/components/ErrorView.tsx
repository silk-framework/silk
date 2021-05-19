import React, {useState} from 'react';
import PropTypes from 'prop-types';

import { Error } from '@eccenca/gui-elements';
import _ from 'lodash';
import {Spacing} from "@gui-elements/index";

export const ErrorCause = ({ errorCause }) => (
    <ul className="ecc-hierarchical-mapping-error-list">
        {_.map(errorCause, ({ title, detail, cause }) => {
            let renderedCause: React.ReactElement | undefined = undefined

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

interface IProps {
    // Error title
    title: string
    // Error detail
    detail: string
    // it may contain a list for errors with title and detail itself
    errorCause?: any[]
    // it may contain a list for errors with title and detail itself, too
    errorIssues?: any[]
    // True if the problem is a HTTP request related problem
    isHTTPProblem?: boolean
    // The status code of the HTTP request if this is a HTTP error
    status?: number
}

/** A component to show error with expandable details. */
export function ErrorView({title, detail, errorCause, errorIssues, isHTTPProblem}: IProps) {
    const [errorExpanded, setErrorExpanded] = useState<boolean>(false)

    const toggleExpansion = () => {
        setErrorExpanded(prev => !prev)
    }

    let shownTitle = title
    if (isHTTPProblem && !status) {
        shownTitle = "There has been a connection problem."
    }
    const errorClassName = errorExpanded
        ? ''
        : 'mdl-alert--narrowed';

    let causes: React.ReactElement | undefined = undefined
    let issues: React.ReactElement | undefined = undefined

    if (errorExpanded && _.isArray(errorCause)) {
        causes = <ErrorCause errorCause={errorCause}/>;
    }

    if (errorExpanded && _.isArray(errorIssues)) {
        issues = <ErrorIssue errorCause={errorIssues}/>;
    }

    const detailHtml = title !== detail ?
        <><Spacing size="small" /><p>{detail}</p></>
        : undefined

    return <Error
        border
        className={errorClassName}
        handlerDismiss={toggleExpansion}
        labelDismiss={
            errorExpanded ? 'Show less' : 'Show more'
        }
        iconDismiss={
            errorExpanded ? 'expand_less' : 'expand_more'
        }
    >
        <strong>{shownTitle}</strong>
        {detailHtml}
        {causes}
        {issues}
    </Error>

}

export default ErrorView;
