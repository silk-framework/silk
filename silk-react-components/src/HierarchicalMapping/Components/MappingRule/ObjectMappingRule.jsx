import React from 'react';
import {
    Button,
    DisruptiveButton,
    CardContent,
    CardActions,
    Radio,
    RadioGroup,
    NotAvailable,
} from '@eccenca/gui-elements';
import _ from 'lodash';
import UseMessageBus from '../../UseMessageBusMixin';
import hierarchicalMappingChannel from '../../store';
import ExampleView from './ExampleView';
import ObjectMappingRuleForm from './Forms/ObjectMappingRuleForm';

import {
    SourcePath,
    ThingName,
    ThingDescription,
    ParentElement,
    InfoBox,
} from './SharedComponents';
import {
    MAPPING_RULE_TYPE_COMPLEX_URI,
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_ROOT,
    MAPPING_RULE_TYPE_URI,
} from '../../helpers';

const ObjectRule = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        parentId: React.PropTypes.string.isRequired,
        type: React.PropTypes.string,
        rules: React.PropTypes.object,
        edit: React.PropTypes.bool.isRequired,
    },
    componentDidMount() {
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.close'),
            this.handleCloseEdit
        );
        if (_.has(this.props, 'rules.uriRule.id')) {
            this.subscribe(
                hierarchicalMappingChannel.request({
                    topic: 'rule.getEditorHref',
                    data: {id: this.props.rules.uriRule.id},
                }),
                ({href}) => this.setState({href})
            );
        }
    },
    editUriRule(event) {
        if (__DEBUG__) {
            event.stopPropagation();
            alert(
                'Normally this would open the complex editor (aka jsplumb view)'
            );
            return false;
        }
        if (this.state.href) {
            window.location.href = this.state.href;
        } else {
            this.createUriRule();
        }
    },
    getOperators(operator, accumulator) {
        if (_.has(operator, 'function')) {
            if (_.has(operator, 'inputs')) {
                _.forEach(
                    operator.inputs,
                    input =>
                        (accumulator = _.concat(
                            accumulator,
                            this.getOperators(input, [])
                        ))
                );
            }
            accumulator.push(operator.function);
        }

        return accumulator;
    },
    getPaths(operator, accumulator) {
        if (_.has(operator, 'path')) {
            accumulator.push(operator.path);
        }
        if (_.has(operator, 'function') && _.has(operator, 'inputs')) {
            _.forEach(
                operator.inputs,
                input =>
                    (accumulator = _.concat(
                        accumulator,
                        this.getPaths(input, [])
                    ))
            );
        }

        return accumulator;
    },
    createUriRule() {
        const rule = _.cloneDeep(this.props);
        rule.rules.uriRule = {
            type: 'uri',
            pattern: '/',
        };
        hierarchicalMappingChannel
            .request({
                topic: 'rule.updateObjectMapping',
                data: rule,
            })
            .subscribe(
                data => {
                    hierarchicalMappingChannel
                        .request({
                            topic: 'rule.getEditorHref',
                            data: {
                                id: data.body.rules.uriRule.id,
                            },
                        })
                        .subscribe(
                            ({href}) => {
                                window.location.href = href;
                            },
                            err => {
                                console.error(err);
                            }
                        );
                },
                err => {
                    console.error(err);
                }
            );
        return false;
    },
    removeUriRule() {
        if (__DEBUG__) {
            event.stopPropagation();
            alert(
                'Normally this would open the complex editor (aka jsplumb view)'
            );
            return false;
        }

        const rule = _.cloneDeep(this.props);
        rule.rules.uriRule = null;
        hierarchicalMappingChannel
            .request({
                topic: 'rule.updateObjectMapping',
                data: rule,
            })
            .subscribe(
                data => {
                    hierarchicalMappingChannel.subject('reload').onNext(true);
                },
                err => {
                    console.error(err);
                }
            );
        return false;
    },
    getInitialState() {
        return {
            edit: !!this.props.edit,
        };
    },
    // open view in edit mode
    handleEdit() {
        this.setState({
            edit: !this.state.edit,
        });
    },
    handleCloseEdit(obj) {
        if (obj.id === this.props.id) {
            this.setState({edit: false});
        }
    },
    componentWillReceiveProps(nextProps) {
        if (_.has(nextProps, 'rules.uriRule.id')) {
            this.subscribe(
                hierarchicalMappingChannel.request({
                    topic: 'rule.getEditorHref',
                    data: {id: _.get(nextProps, 'rules.uriRule.id', '')},
                }),
                ({href}) => this.setState({href})
            );
        }
    },
    handleCopy(){
        this.props.handleCopy(this.props.id);
    },
    handleClone(){
        this.props.handleClone(this.props.id);
    },
    // template rendering
    render() {
        const {type} = this.props;
        const {edit} = this.state;

        if (edit) {
            return (
                <ObjectMappingRuleForm
                    id={this.props.id}
                    parent={this.props.parent}
                    parentId={this.props.parentId}
                />
            );
        }

        let uriPattern = false;

        const uriRuleType = _.get(this, 'props.rules.uriRule.type', false);

        let uriPatternLabel = `URI pattern`;
        let tooltipText;
        let removeButton = (
            <Button
                raised
                iconName="delete"
                className="ecc-silk-mapping__ruleseditor__actionrow-complex-delete"
                onClick={this.removeUriRule}
                tooltip="Reset to default pattern"
            />
        );

        if (uriRuleType === MAPPING_RULE_TYPE_URI) {
            uriPattern = (
                <code>{_.get(this, 'props.rules.uriRule.pattern')}</code>
            );
            tooltipText = 'Convert URI pattern to URI formula';
        } else if (uriRuleType === MAPPING_RULE_TYPE_COMPLEX_URI) {
            const paths = this.getPaths(
                _.get(this.props, 'rules.uriRule.operator', []),
                []
            );
            const operators = this.getOperators(
                _.get(this.props, 'rules.uriRule.operator', []),
                []
            );
            uriPatternLabel = 'URI formula';
            uriPattern = (
                <span>
                    URI uses {paths.length} value{' '}
                    {paths.length > 1 ? 'paths' : 'path'}:&nbsp;
                    <code>{paths.join(', ')}</code>&nbsp;and {operators.length}&nbsp;
                    operator {operators.length > 1 ? 'functions' : 'function'}:&nbsp;<code>
                        {operators.join(', ')}
                    </code>.
                </span>
            );
            tooltipText = 'Edit URI formula';
        } else {
            uriPattern = (
                <NotAvailable label="automatic default pattern" inline />
            );
            tooltipText = 'Create URI formula';
            removeButton = false;
        }

        uriPattern = (
            <div className="ecc-silk-mapping__rulesviewer__idpattern">
                <div className="ecc-silk-mapping__rulesviewer__comment">
                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                            {uriPatternLabel}
                        </dt>
                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                            {uriPattern}
                            <Button
                                raised
                                iconName="edit"
                                className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
                                onClick={this.editUriRule}
                                tooltip={tooltipText}
                            />
                            {removeButton}
                        </dd>
                    </dl>
                </div>
            </div>
        );

        let targetProperty = false;
        let entityRelation = false;
        let deleteButton = false;

        if (type !== MAPPING_RULE_TYPE_ROOT) {
            targetProperty = (
                <div className="ecc-silk-mapping__rulesviewer__targetProperty">
                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                            Target property
                        </dt>
                        <dd>
                            <InfoBox>
                                <div className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                                    <ThingName
                                        id={_.get(
                                            this.props,
                                            'mappingTarget.uri',
                                            undefined
                                        )}
                                    />
                                </div>
                                <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-main">
                                    <code>
                                        {_.get(
                                            this.props,
                                            'mappingTarget.uri',
                                            undefined
                                        )}
                                    </code>
                                </div>
                                <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                                    <ThingDescription
                                        id={_.get(
                                            this.props,
                                            'mappingTarget.uri',
                                            undefined
                                        )}
                                    />
                                </div>
                            </InfoBox>
                        </dd>
                    </dl>
                </div>
            );

            entityRelation = (
                <RadioGroup
                    value={
                        _.get(
                            this.props,
                            'mappingTarget.isBackwardProperty',
                            false
                        )
                            ? 'to'
                            : 'from'
                    }
                    name=""
                    disabled>
                    <Radio
                        value="from"
                        label={
                            <div>
                                Connect from{' '}
                                <ParentElement parent={this.props.parent} />
                            </div>
                        }
                    />
                    <Radio
                        value="to"
                        label={
                            <div>
                                Connect to{' '}
                                <ParentElement parent={this.props.parent} />
                            </div>
                        }
                    />
                </RadioGroup>
            );

            deleteButton = (
                <DisruptiveButton
                    className="ecc-silk-mapping__rulesviewer__actionrow-remove"
                    raised
                    onClick={() =>
                        hierarchicalMappingChannel
                            .subject('removeClick')
                            .onNext({
                                id: this.props.id,
                                uri: this.props.mappingTarget.uri,
                                type: this.props.type,
                                parent: this.props.parentId,
                            })
                    }>
                    Remove
                </DisruptiveButton>
            );
        }

        // TODO: Move up

        return (
            <div>
                <div className="ecc-silk-mapping__rulesviewer">
                    <CardContent>
                        {targetProperty}
                        {entityRelation}
                        {_.get(
                            this.props,
                            'rules.typeRules[0].typeUri',
                            false
                        ) ? (
                            <div className="ecc-silk-mapping__rulesviewer__targetEntityType">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        {this.props.rules.typeRules.length > 1
                                            ? 'Target entity types'
                                            : 'Target entity type'}
                                    </dt>
                                    {this.props.rules.typeRules.map(
                                        (typeRule, idx) => (
                                            <dd key={`TargetEntityType_${idx}`}>
                                                <InfoBox>
                                                    <div className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                                                        <ThingName
                                                            id={
                                                                typeRule.typeUri
                                                            }
                                                        />
                                                    </div>
                                                    <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-main">
                                                        <code>
                                                            {typeRule.typeUri}
                                                        </code>
                                                    </div>
                                                    <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                                                        <ThingDescription
                                                            id={
                                                                typeRule.typeUri
                                                            }
                                                        />
                                                    </div>
                                                </InfoBox>
                                            </dd>
                                        )
                                    )}
                                </dl>
                            </div>
                        ) : (
                            false
                        )}

                        {uriPattern}
                        {this.props.type === MAPPING_RULE_TYPE_OBJECT &&
                        _.get(this.props, 'sourcePath', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__sourcePath">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Value path
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        <SourcePath
                                            rule={{
                                                type: this.props.type,
                                                sourcePath: this.props
                                                    .sourcePath,
                                            }}
                                        />
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        {_.get(this.props, 'rules.uriRule.id', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__examples">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Examples of target data
                                    </dt>
                                    <dd>
                                        <ExampleView
                                            id={this.props.rules.uriRule.id}
                                        />
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        {_.get(this.props, 'metadata.label', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__label">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Label
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        {_.get(
                                            this.props,
                                            'metadata.label',
                                            ''
                                        )}
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        {_.get(this.props, 'metadata.description', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__comment">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Description
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        {_.get(
                                            this.props,
                                            'metadata.description',
                                            ''
                                        )}
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__rulesviewer__actionrow">
                        <Button
                            className="ecc-silk-mapping__rulesviewer__actionrow-edit"
                            raised
                            onClick={this.handleEdit}>
                            Edit
                        </Button>
                        <Button
                            className="ecc-silk-mapping__rulesviewer__actionrow-edit"
                            raised
                            onClick={this.handleCopy}>
                            Copy
                        </Button>
                        <Button
                            className="ecc-silk-mapping__rulesviewer__actionrow-edit"
                            raised
                            onClick={this.handleClone}>
                            Clone
                        </Button>
                        {deleteButton}
                    </CardActions>
                </div>
            </div>
        );
    },
});

export default ObjectRule;
