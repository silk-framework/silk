import React from 'react';
import {Checkbox, NotAvailable} from 'ecc-gui-elements';
import UseMessageBus from '../UseMessageBusMixin';
// import hierarchicalMappingChannel from '../store';
// import _ from 'lodash';

const SuggestionsRule = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {},
    componentDidMount() {},
    getInitialState() {
        return {};
    },
    // template rendering
    render() {
        const {suggestion, checked} = this.props;

        const togglFn = this.props.check.bind(null, suggestion);

        let title = `Click to add the suggested value mapping:\n\nValue path: ${suggestion.sourcePath}`;

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
            <li className="ecc-silk-mapping__ruleitem ecc-silk-mapping__ruleitem--literal">
                <div className="ecc-silk-mapping__ruleitem-summary">
                    <div className="mdl-list__item">
                        <Checkbox
                            onChange={togglFn}
                            checked={checked}
                            className="ecc-silk-mapping__suggestitem-checkbox"
                            ripple
                        />
                        <div
                            className="mdl-list__item-primary-content clickable"
                            title={title}
                            onClick={togglFn}>
                            <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline">
                                {suggestion.sourcePath}
                            </div>
                            <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">
                                {targetProperty}
                            </div>
                        </div>
                    </div>
                </div>
            </li>
        );
    },
});

export default SuggestionsRule;
