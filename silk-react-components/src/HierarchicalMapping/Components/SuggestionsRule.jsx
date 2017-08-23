import React from 'react';
import {Checkbox} from 'ecc-gui-elements';
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
        const {suggestedClass, pos, item, checked} = this.props;

        return (
            <li className="ecc-silk-mapping__ruleitem mdl-list__item ecc-silk-mapping__ruleitem--literal ecc-silk-mapping__ruleitem--summary ">
                <Checkbox
                    onChange={this.props.check.bind(null, suggestedClass, pos)}
                    checked={checked}
                    className="ecc-silk-mapping__suggestitem-checkbox"
                    ripple
                />

                <div
                    className="mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content clickable"
                    title={`Click to add the suggested value mapping:\n\nTarget property: ${suggestedClass}\nValue path: ${item.uri}\nConfidence: ${item.confidence}`}
                    onClick={this.props.check.bind(null, suggestedClass, pos)}>
                    <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline">
                        {suggestedClass}
                    </div>
                    <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">
                        {item.uri}
                    </div>
                </div>
            </li>
        );
    },
});

export default SuggestionsRule;
