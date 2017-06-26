import React from 'react';

import _ from 'lodash';

import hierarchicalMappingChannel from '../../../store';

import Highlighter from 'react-highlight-words';

import {SelectBox, Nothing} from 'ecc-gui-elements';

const Highlight = (props) => {
    const {textToHighlight, searchWord} = props;

    if (!_.isString(textToHighlight) || _.isEmpty(textToHighlight)) {
        return <Nothing/>;
    }

    if (!_.isString(searchWord) || _.isEmpty(searchWord)) {
        return <span>{textToHighlight}</span>;
    }

    return <Highlighter textToHighlight={textToHighlight} searchWords={[searchWord]}/>;

};

const AutoComplete = React.createClass({

    getInitialState(){
        return {}
    },
    optionRender(option) {
        const {label, value, description, $userCreated} = option;

        if ($userCreated) {
            return (
                <div>
                    <strong>{label}</strong>
                </div>
            );
        }

        return (
            <div>
                <strong><Highlight textToHighlight={label} searchWord={this._inputValue}/></strong>
                <small><Highlight textToHighlight={value} searchWord={this._inputValue}/></small>
                <small><Highlight textToHighlight={description} searchWord={this._inputValue}/></small>
            </div>
        );
    },
    newOptionCreator({label, labelKey, valueKey}) {
        return {
            [labelKey]: label,
            [valueKey]: label,
            $userCreated: true,
        }
    },
    render() {

        const {entity, ruleId, ...otherProps} = this.props;

        const loadOptionsRaw = (input, callback) => {

            hierarchicalMappingChannel.request({
                topic: 'autocomplete',
                data: {
                    entity,
                    input,
                    ruleId,
                }
            }).subscribe(({options}) => {
                callback(null, {
                    options,
                    complete: false,
                })
            })

        };


        return (
            <SelectBox {...otherProps}
                       onInputChange={(inputValue) => {
                           this._inputValue = inputValue;
                           return inputValue;
                       }}
                       async={true}
                       optionRenderer={this.optionRender}
                       newOptionCreator={this.newOptionCreator}
                       loadOptions={loadOptionsRaw}
            />
        );
    }

});

export default AutoComplete;