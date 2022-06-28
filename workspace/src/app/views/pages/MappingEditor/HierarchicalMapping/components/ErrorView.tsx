import React, {useState} from 'react';

import { Error } from 'gui-elements-deprecated';
import _ from 'lodash';
import {Spacing} from "@eccenca/gui-elements";

export const ErrorCause = ({ errorCause }) => (
    <ul className="ecc-hierarchical-mapping-error-list">
        {_.map(errorCause, ({ title, detail, cause }) => {
            let renderedCause: React.ReactElement | undefined = undefined

            if (_.isArray(cause)) {
                renderedCause = <ErrorCause errorCause={cause} />;
            }

            return (
                <li key={title}>
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
    cause?: any[]
    // it may contain a list for errors with title and detail itself, too
    issues?: any[]
    // True if the problem is a HTTP request related problem
    isHTTPProblem?: boolean
    // The status code of the HTTP request if this is a HTTP error
    status?: number
    // An title prefix
    titlePrefix?: string
}

/** A component to show error with expandable details. */
export function ErrorView({title, detail, cause, issues, isHTTPProblem, status, titlePrefix = ""}: IProps) {
    const [errorExpanded, setErrorExpanded] = useState<boolean>(false)

    const toggleExpansion = () => {
        setErrorExpanded(prev => !prev)
    }

    let shownTitle = titlePrefix + title
    if (isHTTPProblem && !status) {
        shownTitle = "There has been a connection problem."
    }
    const errorClassName = errorExpanded
        ? ''
        : 'mdl-alert--narrowed';

    let causesHtml: React.ReactElement | undefined = undefined
    let issuesHtml: React.ReactElement | undefined = undefined

    if (errorExpanded && cause) {
        if(_.isArray(cause)) {
            causesHtml = <ErrorCause errorCause={cause}/>;
        } else if(_.isObject(cause)) {
            causesHtml = <ErrorCause errorCause={[cause]}/>
        }
    }

    if (errorExpanded && _.isArray(issues)) {
        issuesHtml = <ErrorIssue errorCause={issues}/>;
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
        {causesHtml}
        {issuesHtml}
    </Error>

}

export default ErrorView;
