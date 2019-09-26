import React from 'react';
import _ from 'lodash';
import { Icon, Button, NotAvailable } from '@eccenca/gui-elements';

import { autocompleteAsync, getVocabInfoAsync } from '../../store';
import {
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_DIRECT,
    MAPPING_RULE_TYPE_OBJECT,
} from '../../helpers';

export const SourcePath = ({ rule }) => {
    const path = _.get(rule, 'sourcePath', <NotAvailable inline />);

    return <span>{_.isArray(path) ? path.join(', ') : path}</span>;
};

class URIInfo extends React.Component {
    state = {
        info: false,
    };

    componentDidMount() {
        this.loadData(this.props);
    }
    componentWillReceiveProps(nextProps) {
        if (!_.isEqual(this.props, nextProps)) {
            this.loadData(nextProps);
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        return (
            !_.isEqual(nextState, this.state) ||
            !_.isEqual(nextProps, this.props)
        );
    }

    loadData(props) {
        const { uri, field } = props;
        getVocabInfoAsync(uri, field)
            .subscribe(
                ({ info }) => {
                    this.setState({ info });
                },
                () => {
                    if (__DEBUG__) {
                        console.warn(`Could not get any info for ${uri}@${field}`);
                    }
                    this.setState({ info: false });
                }
            );
    }

    render() {
        const { info } = this.state;

        if (info) {
            return <span>{info}</span>;
        }

        const {
            uri, fallback, field, ...otherProps
        } = this.props;

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
    }
}

class PropertyTypeInfo extends React.Component {
    state = {
        name: this.props.name,
        option: this.props.option,
        result: false,
    };

    componentDidMount() {
        autocompleteAsync({
            entity: 'propertyType',
            input: this.props.name,
            ruleId: null,
        }).subscribe(
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
                    console.warn(`No ${
                        this.props.option
                    } found for the property type ${this.props.name}`);
                }
                this.setState({
                    result: this.props.name,
                });
            }
        );
    }

    render() {
        let text = this.state.result;
        if (this.props.appendedText) {
            text += this.props.appendedText;
        }
        return <div>{text}</div>;
    }
}

export const ThingName = ({ id, ...otherProps }) => (
    <URIInfo uri={id} {...otherProps} field="label" />
);

export const ThingDescription = ({ id }) => {
    const fallbackInfo = (
        <NotAvailable
            inline
            label="No description available."
        />
    );
    return <URIInfo uri={id} field="description" fallback={fallbackInfo} />;
};

export const PropertyTypeLabel = ({ name, appendedText }) => (
    <PropertyTypeInfo name={name} option="label" appendedText={appendedText} />
);

export const PropertyTypeDescription = ({ name }) => (
    <PropertyTypeInfo name={name} option="description" />
);

export const ThingIcon = ({ type, status, message }) => {
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

export const ParentElement = ({ parent, ...otherProps }) =>
    (_.get(parent, 'type') ? (
        <ThingName id={parent.type} {...otherProps} />
    ) : (
        <span {...otherProps}>parent element</span>
    ));

export const ParentStructure = ({ parent, ...otherProps }) =>
    (_.get(parent, 'property') ? (
        <ThingName id={parent.property} {...otherProps} />
    ) : (
        <ParentElement parent={parent} {...otherProps} />
    ));

export class InfoBox extends React.Component {
    state = {
        expanded: false,
    };

    toggleExpander = event => {
        event.stopPropagation();
        this.setState({
            expanded: !this.state.expanded,
        });
    };

    render() {
        return (
            <div
                className={`ecc-silk-mapping__rulesviewer__infobox${
                    !this.state.expanded ? ' is-narrowed' : ''
                }`}
            >
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
    }
}
