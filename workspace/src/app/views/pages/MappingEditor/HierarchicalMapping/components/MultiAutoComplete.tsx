import React from 'react';
import { AutoCompleteBox } from 'gui-elements-deprecated';
import { autocompleteAsync } from '../store';

const loadOptionsRaw = ({
                            input, callback, ruleId, entity, taskContext
                        }) => {
    autocompleteAsync({
        entity,
        input,
        ruleId,
        taskContext
    }).subscribe(({ options }) => {
        callback(null, {
            options,
            complete: false,
        }, );
    });
};

/** Multi-selection auto-complete component */
const MultiAutoComplete = ({ entity, ruleId, taskContext, ...otherProps }) => {
    return (
        <AutoCompleteBox
            {...otherProps}
            filterOption={() => true}
            async
            multi
            loadOptions={(input, callback) => loadOptionsRaw({
                input,
                callback,
                entity,
                ruleId,
                taskContext
            })}
        />
    );
};

export default MultiAutoComplete;
