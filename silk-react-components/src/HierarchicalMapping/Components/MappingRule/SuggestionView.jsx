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
        const {k, i, item,expanded, checked} = this.props;
        const action = i > 0 ? <div style={{width:'32px'}}>&nbsp;</div> : (
            <Button
                iconName={expanded ? 'expand_less' : 'expand_more'}
                onClick={this.props.expand.bind(null, k, i)}
            />
        );
        return i > 0 && !expanded ? false : <li
            className="ecc-silk-mapping__ruleitem mdl-list__item ecc-silk-mapping__ruleitem--literal ecc-silk-mapping__ruleitem--summary ">
            <Checkbox
                onChange={this.props.check.bind(null, k, i)}
                checked={checked}
                className='ecc-silk-mapping__suggestitem-checkbox'
                ripple={true}/>
            <div className="mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content clickable">
                <div className="ecc-silk-mapping__sug-ruleitem-headline">{k}</div>
                <div className="ecc-silk-mapping__sug-ruleitem-subline">{item.uri}</div>
                <div className="ecc-silk-mapping__sug-ruleitem-subline">Sample data</div>
                <div className="ecc-silk-mapping__sug-ruleitem-lastline">{item.confidence}</div>
                <div className="mdl-list__item-secondary-content" key="action">{action}</div>
            </div>
        </li>
    }
});

export default SuggestionsView;
