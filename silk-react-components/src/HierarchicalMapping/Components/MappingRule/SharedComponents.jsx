import React from 'react';
import _ from 'lodash';
import {Icon, Button, NotAvailable} from '@eccenca/gui-elements';

const NO_TARGET_TYPE = <NotAvailable />;
const NO_TARGET_PROPERTY = <NotAvailable />;
import hierarchicalMappingChannel from '../../store';
import {
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_DIRECT,
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_ROOT,
} from '../../helpers';

export const RuleTitle = ({rule, ...otherProps}) => {
    let uri;
    const label = _.get(rule, 'metadata.label', '');
    if (label) {
        return <span>{label}</span>;
    }
    switch (rule.type) {
        case MAPPING_RULE_TYPE_ROOT:
            uri = _.get(rule, 'rules.typeRules[0].typeUri', false);
            return uri ? (
                <ThingName id={uri} {...otherProps} />
            ) : (
                NO_TARGET_TYPE
            );
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_OBJECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            uri = _.get(rule, 'mappingTarget.uri', false);
            return uri ? (
                <ThingName id={uri} {...otherProps} />
            ) : (
                NO_TARGET_PROPERTY
            );
    }
};

export const RuleTypes = ({rule, ...otherProps}) => {
    switch (rule.type) {
        case MAPPING_RULE_TYPE_OBJECT:
            let types = _.get(rule, 'rules.typeRules', []);
            types = _.isEmpty(types)
                ? NO_TARGET_TYPE
                : types
                      .map(({typeUri}) => (
                          <ThingName id={typeUri} key={typeUri} />
                      ))
                      .reduce((prev, curr) => [prev, ', ', curr]);
            return <span {...otherProps}>{types}</span>;
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            let appendText = _.get(rule, 'mappingTarget.valueType.lang', '');
            if(appendText !== '') { // add language tag if available
                appendText = ' (' + appendText + ')';
            }
            return (
                <span {...otherProps}>
                    {_.get(
                        rule,
                        'mappingTarget.valueType.nodeType',
                        NO_TARGET_TYPE
                    ) + appendText}
                </span>
            );
        case MAPPING_RULE_TYPE_ROOT:
            return <span />;
    }
};

export const SourcePath = ({rule}) => {
    const path = _.get(rule, 'sourcePath', <NotAvailable inline />);

    return <span>{_.isArray(path) ? path.join(', ') : path}</span>;
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
            return <span>{info}</span>;
        }

        const {uri, fallback, field, ...otherProps} = this.props;

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

        return <span {...otherProps}>{noInfo}</span>;
    },
});

const PropertyTypeInfo = React.createClass({
    getInitialState() {
        if (__DEBUG__) {
            console.log(this.props);
        }

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
                            `No ${
                                this.props.option
                            } found for the property type ${this.props.name}`
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
        let text = this.state.result;
        if(this.props.appendedText) {
            text = text + this.props.appendedText;
        }
        return <div>{text}</div>;
    },
});

export const ThingName = ({id, ...otherProps}) => (
    <URIInfo uri={id} {...otherProps} field="label" />
);

export const ThingDescription = ({id}) => {
    const fallbackInfo = (
        <NotAvailable
            inline
            label="No description available."
        />
    );
    return <URIInfo uri={id} field="description" fallback={fallbackInfo} />;
};

export const PropertyTypeLabel = ({name, appendedText}) => (
    <PropertyTypeInfo name={name} option="label" appendedText={appendedText} />
);

export const PropertyTypeDescription = ({name}) => (
    <PropertyTypeInfo name={name} option="description" />
);

export const ThingIcon = ({type, status, message}) => {
    let iconName = 'help_outline';
    let tooltip = '';
    switch (type) {
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            tooltip = 'Value mapping';
            iconName = 'insert_drive_file';
            break;
        case MAPPING_RULE_TYPE_OBJECT:
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

export const ParentElement = ({parent, ...otherProps}) =>
    _.get(parent, 'type') ? (
        <ThingName id={parent.type} {...otherProps} />
    ) : (
        <span {...otherProps}>parent element</span>
    );

export const ParentStructure = ({parent, ...otherProps}) =>
    _.get(parent, 'property') ? (
        <ThingName id={parent.property} {...otherProps} />
    ) : (
        <ParentElement parent={parent} {...otherProps} />
    );

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
                className={`ecc-silk-mapping__rulesviewer__infobox${
                    !this.state.expanded ? ' is-narrowed' : ''
                }`}>
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
