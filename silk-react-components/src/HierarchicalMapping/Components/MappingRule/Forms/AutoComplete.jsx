import React from 'react';
import { AutoCompleteBox } from '@eccenca/gui-elements';
import { autocompleteAsync } from '../../../store';

const loadOptionsRaw = ({
    input, callback, ruleId, entity,
}) => {
    autocompleteAsync({
        entity,
        input,
        ruleId,
    }).subscribe(({ options }) => {
        callback(null, {
            options,
            complete: false,
        }, );
    });
};

const AutoComplete = ({ entity, ruleId, ...otherProps }) => {
    return (
        <AutoCompleteBox
            {...otherProps}
            filterOption={() => true}
            async
            loadOptions={(input, callback) => loadOptionsRaw({
                input,
                callback,
                entity,
                ruleId,
            })}
        />
    );
};

export default AutoComplete;
