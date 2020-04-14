/*
    provides a simple interface for alert dialogs
*/

import React from 'react';
import * as IntentClassNames from './../Intent/classnames';
import SimpleDialog from './SimpleDialog';

function AlertDialog({
    children,
    success=false,
    warning=false,
    danger=false,
    ...otherProps
}: any) {
    let intentLevel = IntentClassNames.INFO;
    if (success) { intentLevel = IntentClassNames.SUCCESS; }
    if (warning) { intentLevel = IntentClassNames.WARNING; }
    if (danger) { intentLevel = IntentClassNames.DANGER; }

    return (
        <SimpleDialog
            size="tiny"
            preventSimpleClosing={true}
            intent={intentLevel}
            {...otherProps}
        >
            {children}
        </SimpleDialog>
    );
};

export default AlertDialog;
