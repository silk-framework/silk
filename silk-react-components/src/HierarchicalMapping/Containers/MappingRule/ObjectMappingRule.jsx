import React from 'react';
import PropTypes from 'prop-types';
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
import { getEditorHref, updateObjectMappingAsync } from '../../store';
import ExampleView from './ExampleView';
import ObjectMappingRuleForm from './ObjectMappingRuleForm';

import {
    ParentElement,
    } from '../../Components/ParentElement';
import {
    MAPPING_RULE_TYPE_ROOT,
    } from '../../utils/constants';
import {
    isClonableRule,
    isCopiableRule,
    MAPPING_RULE_TYPE_COMPLEX_URI, MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_URI,
    MESSAGES
} from '../../utils/constants';
import transformRuleOfObjectMapping from '../../utils/transformRuleOfObjectMapping';
import EventEmitter from '../../utils/EventEmitter';
import { ThingName } from '../../Components/ThingName';
import { ThingDescription } from '../../Components/ThingDescription';
import { InfoBox } from '../../Components/InfoBox';
import { SourcePath } from '../../Components/SourcePath';

class ObjectRule extends React.Component {
    static propTypes = {
        parentId: PropTypes.string.isRequired,
        parent: PropTypes.object,
        edit: PropTypes.oneOfType([
            PropTypes.bool,
            PropTypes.object
        ]).isRequired,
        ruleData: PropTypes.object.isRequired,
    };
    
    state = {
        edit: !!this.props.edit,
        href: '',
    };
    
    constructor(props) {
        super(props);
        this.editUriRule = this.editUriRule.bind(this);
        this.removeUriRule = this.removeUriRule.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        this.handleCloseEdit = this.handleCloseEdit.bind(this);
        this.handleCopy = this.handleCopy.bind(this);
        this.handleClone = this.handleClone.bind(this);
    }
    
    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
        if (_.has(this.props, 'ruleData.rules.uriRule.id')) {
            this.setState({
                href: getEditorHref(this.props.ruleData.rules.uriRule.id)
            });
        }
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
    }

    componentWillReceiveProps(nextProps) {
        if (_.has(nextProps, 'ruleData.rules.uriRule.id')) {
            this.setState({
                href: getEditorHref(_.get(nextProps, 'ruleData.rules.uriRule.id', ''))
            })
        }
    }

    editUriRule(event) {
        if (__DEBUG__) {
            event.stopPropagation();
            alert('Normally this would open the complex editor (aka jsplumb view)');
            return false;
        }
        if (this.state.href) {
            window.location.href = this.state.href;
        } else {
            this.createUriRule();
        }
    };

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
    }

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
    }

    createUriRule() {
        const rule = _.cloneDeep(this.props.ruleData);
        rule.rules.uriRule = {
            type: 'uri',
            pattern: '/',
        };
        updateObjectMappingAsync(rule)
            .subscribe(
                data => {
                    const href = getEditorHref(data.body.rules.uriRule.id);
                    if (href) {
                        window.location.href = href;
                    }
                },
                err => {
                    console.error(err);
                }
            );
        return false;
    }

    removeUriRule() {
        if (__DEBUG__) {
            event.stopPropagation();
            alert('Normally this would open the complex editor (aka jsplumb view)');
            return false;
        }
        
        const rule = _.cloneDeep(this.props.ruleData);
        const callbackFn = () => {
            rule.rules.uriRule = null;
            updateObjectMappingAsync(rule)
                .subscribe(
                    () => {
                        EventEmitter.emit(MESSAGES.RELOAD, true);
                    },
                    err => {
                        console.error(err);
                    }
                );
        };
        this.props.onClickedRemove(null, callbackFn);
        return false;
    };

    // open view in edit mode
    handleEdit() {
        this.setState({
            edit: !this.state.edit,
        });
    };
    
    handleCloseEdit = (obj) => {
        if (obj.id === this.props.ruleData.id) {
            this.setState({ edit: false });
        }
    };
    
    handleCopy = () => {
        this.props.handleCopy(this.props.ruleData.id, this.props.ruleData.type);
    };
    
    handleClone = () => {
        this.props.handleClone(this.props.ruleData.id, this.props.ruleData.type, this.props.parentId);
    };

    // template rendering
    render() {
        const { type, ruleData } = this.props;
        const { edit } = this.state;

        if (edit) {
            return (
                <ObjectMappingRuleForm
                    id={this.props.ruleData.id}
                    parent={this.props.parent}
                    parentId={this.props.parentId}
                    ruleData={transformRuleOfObjectMapping(ruleData)}
                />
            );
        }

        let uriPattern = false;
        
        const uriRuleType = _.get(ruleData, 'rules.uriRule.type', false);
        
        let uriPatternLabel = 'URI pattern';
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
                <code>{_.get(ruleData, 'rules.uriRule.pattern')}</code>
            );
            tooltipText = 'Convert URI pattern to URI formula';
        } else if (uriRuleType === MAPPING_RULE_TYPE_COMPLEX_URI) {
            const paths = this.getPaths(
                _.get(ruleData, 'rules.uriRule.operator', []),
                []
            );
            const operators = this.getOperators(
                _.get(ruleData, 'rules.uriRule.operator', []),
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
        
        const copyButton = isCopiableRule(this.props.ruleData.type) &&
            <Button
                className="ecc-silk-mapping__rulesviewer__actionrow-copy"
                raised
                onClick={this.handleCopy}
            >
                Copy
            </Button>;
        
        const cloneButton = isClonableRule(this.props.ruleData.type) &&
            <Button
                className="ecc-silk-mapping__rulesviewer__actionrow-clone"
                raised
                onClick={this.handleClone}
            >
                Clone
            </Button>;

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
                                            'ruleData.mappingTarget.uri',
                                            undefined
                                        )}
                                    />
                                </div>
                                <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-main">
                                    <code>
                                        {_.get(
                                            this.props,
                                            'ruleData.mappingTarget.uri',
                                            undefined
                                        )}
                                    </code>
                                </div>
                                <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                                    <ThingDescription
                                        id={_.get(
                                            this.props,
                                            'ruleData.mappingTarget.uri',
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
                            'ruleData.mappingTarget.isBackwardProperty',
                            false
                        )
                            ? 'to'
                            : 'from'
                    }
                    name=""
                    disabled
                >
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
                        this.props.onClickedRemove({
                            id: this.props.ruleData.id,
                            uri: this.props.ruleData.mappingTarget.uri,
                            type: this.props.ruleData.type,
                            parent: this.props.parentId,
                        })
                    }
                >
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
                             ruleData,
                            'rules.typeRules[0].typeUri',
                            false
                        ) ? (
                            <div className="ecc-silk-mapping__rulesviewer__targetEntityType">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        {ruleData.rules.typeRules.length > 1
                                            ? 'Target entity types'
                                            : 'Target entity type'}
                                    </dt>
                                    {ruleData.rules.typeRules.map((typeRule, idx) => (
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
                                    ))}
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        
                        {uriPattern}
                        {this.props.type === MAPPING_RULE_TYPE_OBJECT &&
                        _.get(this.props, 'ruleData.sourcePath', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__sourcePath">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Value path
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        <SourcePath
                                            rule={{
                                                type: this.props.ruleData.type,
                                                sourcePath: this.props.ruleData.sourcePath,
                                            }}
                                        />
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        {_.get(ruleData, 'rules.uriRule.id', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__examples">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Examples of target data
                                    </dt>
                                    <dd>
                                        <ExampleView
                                            id={ruleData.rules.uriRule.id}
                                        />
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        {_.get(this.props, 'ruleData.metadata.label', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__label">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Label
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        {_.get(
                                            this.props,
                                            'ruleData.metadata.label',
                                            ''
                                        )}
                                    </dd>
                                </dl>
                            </div>
                        ) : (
                            false
                        )}
                        {_.get(this.props, 'ruleData.metadata.description', false) ? (
                            <div className="ecc-silk-mapping__rulesviewer__comment">
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Description
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        {_.get(
                                            this.props,
                                            'ruleData.metadata.description',
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
                            onClick={this.handleEdit}
                        >
                            Edit
                        </Button>
                        {copyButton}
                        {cloneButton}
                        {deleteButton}
                    </CardActions>
                </div>
            </div>
        );
    }
}

export default ObjectRule;
