import React from 'react';
import {AutoCompleteBox} from '@eccenca/gui-elements';
import hierarchicalMappingChannel from '../../../store';

const AutoComplete = React.createClass({
    render() {
        const {entity, ruleId, ...otherProps} = this.props;

        const loadOptionsRaw = (input, callback) => {
            hierarchicalMappingChannel
                .request({
                    topic: 'autocomplete',
                    data: {
                        entity,
                        input,
                        ruleId,
                    },
                })
                .subscribe(({options}) => {
                    callback(null, {
                        options,
                        complete: false,
                    });
                });
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
