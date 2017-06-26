import React from 'react';
import _ from 'lodash';
import {Icon, Button} from 'ecc-gui-elements';
const NO_TARGET_TYPE = '(No target type)';
const NO_TARGET_PROPERTY = '(No target property)';

export const RuleTitle = ({rule}) => {

    switch (rule.type) {
    case 'root':
        const title = _.get(rule, 'rules.typeRules[0].typeUri', NO_TARGET_TYPE);
        return (<span>{_.isEmpty(title) ? '[no type]' : title}</span>);
    case 'direct':
    case 'object':
        return (<span>{_.get(rule, 'mappingTarget.uri', NO_TARGET_PROPERTY)}</span>);
    case 'complex':
        //TODO: Complex Mappings need better titles
        return 'Complex Mapping';
        return <span>Complex Mapping</span>
    }

};


export const RuleTypes = ({rule}) => {

    switch (rule.type) {
    case 'object':
        let types = _.get(rule, 'rules.typeRules', []).map(({typeUri}) => typeUri);
        types = _.isEmpty(types) ? NO_TARGET_TYPE : types.join(', ');
        return <span>{types}</span>;
    case 'direct':
    case 'complex':
        return <span>{_.get(rule, 'mappingTarget.valueType.nodeType', NO_TARGET_TYPE)}</span>;
    case 'root':
        return <span/>;
    }

};

export const SourcePath = ({rule}) => {
    const path = _.get(rule, 'sourcePath', '(No value path)');

    return <span>{_.isArray(path) ? path.join(', ') : path}</span>;

};

export const RuleTreeTitle = ({rule}) => {
    const childCount = _.get(rule, 'rules.propertyRules', []).length;
    switch (rule.type) {
        case 'root':
            const title = _.get(rule, 'rules.typeRules[0].typeUri', NO_TARGET_TYPE);
            return (<span>{_.isEmpty(title) ? '[no type]' : title} ({childCount})</span>);
        case 'object':
            return (<span>{_.get(rule, 'mappingTarget.uri', NO_TARGET_PROPERTY)} ({childCount})</span>);
        default:
            return false;
    }

};

export const RuleTreeTypes = ({rule}) => {

    switch (rule.type) {
        case 'object':
            let types = _.get(rule, 'rules.typeRules', []).map(({typeUri}) => typeUri);
            types = _.isEmpty(types) ? NO_TARGET_TYPE : types.join(', ');
            return <span>{types}</span>;
        case 'root':
        default:
            return false;
    }

};

export const ThingName = ({id, prefixString, suffixString}) => {
    return <span>
        {
            prefixString ? prefixString : false
        }
        {id} (TODO: readable name)
        {
            suffixString ? suffixString : false
        }
    </span>
}

export const ThingDescription = ({id}) => {
    return <p>TODO: Include vocabulary description about {id}</p>
}

export const ThingIcon = ({type, status, message}) => {
    let iconName = 'help_outline';
    let tooltip= '';
    switch(type) {
        case 'direct':
        case 'complex':
            tooltip = 'Value mapping';
            iconName = "insert_drive_file";
            break;
        case 'object':
            tooltip = 'Object mapping';
            iconName = "folder";
            break;
        default:
            iconName = 'help_outline';
    }



    return <Icon
        className='ecc-silk-mapping__ruleitem-icon'
        name={status === 'error' ? 'warning' : iconName}
        tooltip={status === 'error' ? tooltip + ' (' + message + ')' : tooltip}
    />
}

export const FloatingListActions = React.createClass({

    propTypes: {
        actions: React.PropTypes.array.isRequired,
        iconName: React.PropTypes.string.isRequired,
    },

    getInitialState() {
        return {
            activeFAB: false,
        };
    },

    handleFAB(event) {
        event.stopPropagation();
        this.setState({
            activeFAB: !this.state.activeFAB,
        })
    },

    render () {

        const {iconName, actions} = this.props;

        if (!actions || actions.length < 1) {
            return false;
        }

        return <div
            className="ecc-silk-mapping__ruleslist-floatingactions"
        >
            <Button
                className={
                    "ecc-silk-mapping__ruleslist-floatingactions--trigger" +
                    (this.state.activeFAB ? ' is-active' : '')
                }
                iconName="add"
                fabSize="large"
                colored
                tooltip={(actions.length > 1) ? false : actions[0].label}
                onClick={(actions.length > 1) ? this.handleFAB : actions[0].handler}
            />
            {
                actions.length > 1 ? (
                    <ul
                        className="ecc-silk-mapping__ruleslist-floatingactions--list mdl-menu mdl-shadow--2dp"
                    >
                        {
                            _.map(actions, (action, idx) =>
                                (
                                    <li
                                        key={'FloatingAction_' + idx}
                                    >
                                        <button
                                            className="mdl-menu__item"
                                            onClick={action.handler}
                                        >
                                            {
                                                action.icon ? <Icon name={action.icon} /> : false
                                            }
                                            {action.label}
                                        </button>
                                    </li>
                                )
                            )
                        }
                    </ul>
                ) : false
            }
            {
                (actions.length > 1 && this.state.activeFAB) ?
                    <div
                        className="ecc-silk-mapping__ruleslist-floatingactions--list-backdrop"
                        onMouseOver={this.handleFAB}
                    /> : false
            }
        </div>;
    },
})
