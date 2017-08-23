import React from 'react';
import _ from 'lodash';
import {Icon, Button, NotAvailable} from 'ecc-gui-elements';

const NO_TARGET_TYPE = <NotAvailable />;
const NO_TARGET_PROPERTY = <NotAvailable />;
import hierarchicalMappingChannel from '../../store';

export const RuleTitle = ({rule}) => {
    let uri;

    switch (rule.type) {
        case 'root':
            uri = _.get(rule, 'rules.typeRules[0].typeUri', false);
            return uri ? <ThingName id={uri} /> : NO_TARGET_TYPE;
        case 'direct':
        case 'object':
            uri = _.get(rule, 'mappingTarget.uri', false);
            return uri ? <ThingName id={uri} /> : NO_TARGET_PROPERTY;
        case 'complex':
            // TODO: Complex Mappings need better titles
            return 'Complex Mapping';
            return <span>Complex Mapping</span>;
    }
};

export const RuleTypes = ({rule}) => {
    switch (rule.type) {
        case 'object':
            let types = _.get(rule, 'rules.typeRules', []);
            types = _.isEmpty(types)
                ? NO_TARGET_TYPE
                : types
                      .map(({typeUri}) =>
                          <ThingName id={typeUri} key={typeUri} />
                      )
                      .reduce((prev, curr) => [prev, ', ', curr]);
            return (
                <span>
                    {types}
                </span>
            );
        case 'direct':
        case 'complex':
            return (
                <span>
                    {_.get(
                        rule,
                        'mappingTarget.valueType.nodeType',
                        NO_TARGET_TYPE
                    )}
                </span>
            );
        case 'root':
            return <span />;
    }
};

export const SourcePath = ({rule}) => {
    const path = _.get(rule, 'sourcePath', <NotAvailable inline />);

    return (
        <span>
            {_.isArray(path) ? path.join(', ') : path}
        </span>
    );
};

export const RuleTreeTitle = ({rule}) => {
    const childCount = _.get(rule, 'rules.propertyRules', []).length;

    return (
        <span>
            <RuleTitle rule={rule} /> ({childCount})
        </span>
    );
};

export const RuleTreeTypes = ({rule}) => <RuleTypes rule={rule} />;

const URIInfo = React.createClass({
    getInitialState() {
        this.loadData(this.props);

        return {
            info: false,
        };
    },
    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(this.props, nextProps)) {
            this.loadData(nextProps);
        }
    },
    shouldComponentUpdate(nextProps, nextState) {
        return (
            !_.isEqual(nextState, this.state) ||
            !_.isEqual(nextProps, this.props)
        );
    },
    loadData(props) {
        const {uri, field} = props;

        hierarchicalMappingChannel
            .request({
                topic: 'vocabularyInfo.get',
                data: {
                    uri,
                    field,
                },
            })
            .subscribe(
                ({info}) => {
                    this.setState({
                        info,
                    });
                },
                () => {
                    if (__DEBUG__) {
                        console.warn(
                            `Could not get any info for ${uri}@${field}`
                        );
                    }
                    this.setState({
                        info: false,
                    });
                }
            );
    },
    render() {
        const {info} = this.state;

        if (info) {
            return (
                <span>
                    {info}
                </span>
            );
        }

        const {uri, fallback, field} = this.props;

        let noInfo = false;

        if (fallback !== undefined) {
            noInfo = fallback;
        } else if (!_.isString(uri)) {
            noInfo = <NotAvailable />;
        } else if (field === 'label') {
            const lastHash = uri.lastIndexOf('#');
            const lastSlash = lastHash === -1 ? uri.lastIndexOf('/') : lastHash;
            noInfo = uri.substring(lastSlash + 1).replace(/[<>]/g, '');
        }

        return (
            <span>
                {noInfo}
            </span>
        );
    },
});

const PropertyTypeInfo = React.createClass({
    getInitialState() {
        console.log(this.props);
        hierarchicalMappingChannel
            .request({
                topic: 'autocomplete',
                data: {
                    entity: 'propertyType',
                    input: this.props.name,
                    ruleId: null,
                },
            })
            .subscribe(
                response => {
                    this.setState({
                        result: _.get(
                            response,
                            ['options', '0', this.props.option],
                            this.props.name
                        ),
                    });
                },
                () => {
                    if (__DEBUG__) {
                        console.warn(
                            `No ${this.props
                                .option} found for the property type ${this
                                .props.name}`
                        );
                    }
                    this.setState({
                        result: this.props.name,
                    });
                }
            );

        return {
            name: this.props.name,
            option: this.props.option,
            result: false,
        };
    },
    render() {
        return (
            <div>
                {this.state.result}
            </div>
        );
    },
});

export const ThingName = ({id}) => <URIInfo uri={id} field="label" />;

export const ThingDescription = ({id}) => {
    const fallbackInfo = (
        <NotAvailable
            inline
            label="No description available."
            description={false}
        />
    );
    return <URIInfo uri={id} field="description" fallback={fallbackInfo} />;
};

export const PropertyTypeLabel = ({name}) =>
    <PropertyTypeInfo name={name} option="label" />;

export const PropertyTypeDescription = ({name}) =>
    <PropertyTypeInfo name={name} option="description" />;

export const ThingIcon = ({type, status, message}) => {
    let iconName = 'help_outline';
    let tooltip = '';
    switch (type) {
        case 'direct':
        case 'complex':
            tooltip = 'Value mapping';
            iconName = 'insert_drive_file';
            break;
        case 'object':
            tooltip = 'Object mapping';
            iconName = 'folder';
            break;
        default:
            iconName = 'help_outline';
    }

    return (
        <Icon
            className="ecc-silk-mapping__ruleitem-icon"
            name={status === 'error' ? 'warning' : iconName}
            tooltip={status === 'error' ? `${tooltip} (${message})` : tooltip}
        />
    );
};

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
        });
    },

    render() {
        const {iconName, actions} = this.props;

        if (!actions || actions.length < 1) {
            return false;
        }

        return (
            <div className="ecc-silk-mapping__ruleslist-floatingactions">
                <Button
                    className={`ecc-silk-mapping__ruleslist-floatingactions--trigger${this
                        .state.activeFAB
                        ? ' is-active'
                        : ''}`}
                    iconName="add"
                    fabSize="large"
                    colored
                    tooltip={actions.length > 1 ? false : actions[0].label}
                    onClick={
                        actions.length > 1 ? this.handleFAB : actions[0].handler
                    }
                />
                {actions.length > 1
                    ? <ul className="ecc-silk-mapping__ruleslist-floatingactions--list mdl-menu mdl-shadow--2dp">
                          {_.map(actions, (action, idx) =>
                              <li key={`FloatingAction_${idx}`}>
                                  <button
                                      className="mdl-menu__item"
                                      onClick={action.handler}>
                                      {action.icon
                                          ? <Icon name={action.icon} />
                                          : false}
                                      {action.label}
                                  </button>
                              </li>
                          )}
                      </ul>
                    : false}
                {actions.length > 1 && this.state.activeFAB
                    ? <div
                          className="ecc-silk-mapping__ruleslist-floatingactions--list-backdrop"
                          onMouseOver={this.handleFAB}
                      />
                    : false}
            </div>
        );
    },
});

export const ParentElement = ({parent}) =>
    _.get(parent, 'type')
        ? <ThingName id={parent.type} />
        : <span>parent element</span>;

export const InfoBox = React.createClass({
    getInitialState() {
        return {
            expanded: false,
        };
    },

    toggleExpander(event) {
        event.stopPropagation();
        this.setState({
            expanded: !this.state.expanded,
        });
    },

    render() {
        return (
            <div
                className={`ecc-silk-mapping__rulesviewer__infobox${!this.state
                    .expanded
                    ? ' is-narrowed'
                    : ''}`}>
                <Button
                    className="ecc-silk-mapping__rulesviewer__infobox-toggler"
                    iconName={
                        this.state.expanded ? 'expand_more' : 'chevron_right'
                    }
                    tooltip={this.state.expanded ? 'Show less' : 'Show more'}
                    onClick={this.toggleExpander}
                />
                <div className="ecc-silk-mapping__rulesviewer__infobox-content">
                    {this.props.children}
                </div>
            </div>
        );
    },
});
