import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Spinner,
    Error,
    Checkbox,
    Button,
    ContextMenu,
    MenuItem,
    Chip,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';


const SuggestionsView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {

    },
    componentDidMount() {

    },
    getInitialState() {
        return {
        };
    },
    // template rendering
    render () {
        const {suggestedClass, order, item, checked} = this.props;

        return <li
            className="ecc-silk-mapping__ruleitem mdl-list__item ecc-silk-mapping__ruleitem--literal ecc-silk-mapping__ruleitem--summary ">
            <Checkbox
                onChange={this.props.check.bind(null, suggestedClass, order)}
                checked={checked}
                className='ecc-silk-mapping__suggestitem-checkbox'
                ripple={true}/>

            <div
                className="mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content"
                onClick={this.props.check.bind(null, suggestedClass, order)}
            >

                <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline">{suggestedClass}</div>
                <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">{item.uri}</div>
                <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">{item.confidence}</div>
            </div>
        </li>
    }
});

export default SuggestionsView;
