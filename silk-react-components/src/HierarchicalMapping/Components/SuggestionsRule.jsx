import React from 'react';
import {SelectBox, Checkbox, NotAvailable} from '@eccenca/gui-elements';
import UseMessageBus from '../UseMessageBusMixin';
import {SUGGESTION_TYPES, LABELED_SUGGESTION_TYPES} from '../helpers';
import _ from 'lodash';

const SuggestionsRule = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {},
    componentDidMount() {},
    shouldComponentUpdate(nextProps, nextState) {
        return !_.isEqual(nextProps.suggestion, this.props.suggestion);
    },
    preventPropagation(event) {
        event.stopPropagation();
    },
    onChangeChecked() {
        this.props.onChecked({
            id: this.props.suggestion.id,
            checked: !this.props.suggestion.checked,
        });
    },
    onChangeType({value, label}) {
        this.props.onTypeChanged({
            id: this.props.suggestion.id,
            type: value,
        });
    },
    // template rendering
    render() {
        const {suggestion} = this.props;
        let title = `Click to add the suggested value mapping:\n\nValue path: ${
            suggestion.sourcePath
        }`;

        let targetProperty;

        if (suggestion.targetProperty) {
            targetProperty = suggestion.targetProperty;
            title += `\nTarget property: ${suggestion.targetProperty}`;
        } else {
            targetProperty = <NotAvailable label="(default mapping)" inline />;
            title += `\nTarget property: default mapping`;
        }

        if (suggestion.confidence) {
            title += `\nConfidence: ${suggestion.confidence}`;
        }

        return (
            <li
                className={`ecc-silk-mapping__ruleitem ecc-silk-mapping__ruleitem--literal ${
                    suggestion.checked ? 'selected' : 'unselected'
                }`}>
                <div className="ecc-silk-mapping__ruleitem-summary">
                    <div className="mdl-list__item">
                        <div className="ecc-silk-mapping__suggestitem-checkbox">
                            <Checkbox
                                onChange={this.onChangeChecked}
                                checked={suggestion.checked}
                                title="Select all"
                            />
                        </div>
                        <div
                            className="mdl-list__item-primary-content clickable"
                            title={title}
                            onClick={this.onChangeChecked}>
                            <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline">
                                {suggestion.sourcePath}
                                <div className="ecc-silk-mapping__ruleitem-extraline ecc-silk-mapping__ruleitem-url">
                                    http://resource.example.net/this_is_a_dummy/resource#urlhash
                                </div>
                            </div>
                            <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">
                                {targetProperty}
                            </div>
                            <div
                                className={`ecc-silk-mapping__suggestitem-typeselect`}
                                onClick={this.preventPropagation}>
                                <SelectBox
                                    disabled={!suggestion.checked}
                                    options={LABELED_SUGGESTION_TYPES}
                                    onChange={this.onChangeType}
                                    value={{
                                        value: suggestion.type,
                                        label: LABELED_SUGGESTION_TYPES.filter(
                                            v => v.value === suggestion.type
                                        )[0].label,
                                    }}
                                    clearable={false}
                                />
                            </div>
                        </div>
                    </div>
                </div>
            </li>
        );
    },
});

export default SuggestionsRule;
