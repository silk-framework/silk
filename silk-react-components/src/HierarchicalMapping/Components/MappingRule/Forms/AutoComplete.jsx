import React from 'react';
import { AutoCompleteBox } from '@eccenca/gui-elements';
import hierarchicalMappingChannel from '../../../store';
import { MESSAGES } from '../../../constants';

const AutoComplete = React.createClass({
    render() {
        const { entity, ruleId, ...otherProps } = this.props;

        const loadOptionsRaw = (input, callback) => {
            hierarchicalMappingChannel
                .request({
                    topic: MESSAGES.AUTOCOMPLETE,
                    data: {
                        entity,
                        input,
                        ruleId,
                    },
                })
                .subscribe(
                    ({ options }) => {
                        callback(null, {
                            options,
                            complete: false,
                        }, );
                    },
                    error => {
                    }
                );
        };

        return (
            <AutoCompleteBox
                {...otherProps}
                filterOption={() => true}
                async
                loadOptions={loadOptionsRaw}
            />
        );
    },
});

export default AutoComplete;
