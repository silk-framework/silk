import React from 'react';

import _ from 'lodash';

import {Error} from 'ecc-gui-elements';

const FormSaveError = props => {
    const errorResponse = _.get(props, 'error.response.body', {
        message: 'An unknown error occurred. Please try again.',
        issues: [],
    });

    const issues = _.chain(errorResponse.issues)
        .map(issue => issue.message)
        .reject(_.isEmpty)
        .value();

    return (
        <Error border vertSpacing>
            <strong>
                {errorResponse.message}
            </strong>
            {_.isEmpty(issues)
                ? false
                : <pre>
                      {issues.join('\n')}
                  </pre>}
        </Error>
    );
};

export default FormSaveError;
