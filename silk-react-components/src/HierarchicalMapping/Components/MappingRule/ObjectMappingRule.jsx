import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Button,
    Radio,
    RadioGroup,
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
    Info,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';
import ExampleView from './ExampleView';
import ObjectMappingRuleForm from './Forms/ObjectMappingRuleForm';

import {
    SourcePath,
    ThingName,
    ThingDescription
} from './SharedComponents';

const RuleObjectView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        parent: React.PropTypes.string.isRequired,
        parentName: React.PropTypes.string.isRequired,
        type: React.PropTypes.string,
        rules: React.PropTypes.object,
        edit: React.PropTypes.bool.isRequired,
    },
    componentDidMount() {
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.close'), this.handleCloseEdit);
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
        })
    },
    handleCloseEdit(obj) {
        if (obj.id === this.props.id)
            this.setState({edit: false})
    },
    // template rendering
    render () {
        const {
            type,
        } = this.props;
        const {edit} = this.state;

        if (edit) {
            return <ObjectMappingRuleForm
                id={this.props.id}
                parentName={this.props.parentName}
                parentId={this.props.parentId}
            />
        }

        let targetProperty = false;
        let entityRelation = false;
        let deleteButton = false;

        if (type !== 'root') {
            targetProperty = (
                (
                    <div
                        className="ecc-silk-mapping__rulesviewer__targetProperty"
                    >
                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                Target property
                            </dt>
                            <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                <ThingName id={_.get(this.props, 'mappingTarget.uri', undefined)}/>
                            </dd>
                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                <code>{_.get(this.props, 'mappingTarget.uri', undefined)}</code>
                            </dd>
                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                <Info border>
                                    <ThingDescription id={_.get(this.props, 'mappingTarget.uri', undefined)}/>
                                </Info>
                            </dd>
                        </dl>
                    </div>
                )
            );

            entityRelation = (
                <RadioGroup
                    value={_.get(this.props, 'mappingTarget.isBackwardProperty', false) ? 'to' : 'from'}
                    name=""
                    disabled={true}
                >
                    <Radio
                        value="from"
                        label={<div>Connect from {<ThingName id={this.props.parentName}
                                                              prefixString="parent element "/>}</div>}
                    />
                    <Radio
                        value="to"
                        label={<div>Connect to {<ThingName id={this.props.parentName}
                                                            prefixString="parent element "/>}</div>}
                    />
                </RadioGroup>
            );


            deleteButton = (
                <DisruptiveButton
                    className="ecc-silk-mapping__rulesviewer__actionrow-remove"
                    onClick={
                        () => hierarchicalMappingChannel.subject(
                            'removeClick'
                        ).onNext(
                            {
                                id: this.props.id,
                                uri: this.props.mappingTarget.uri,
                                type: this.props.type,
                                parent: this.props.parent
                            }
                        )
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
                    <div className="mdl-card__content">
                        {targetProperty}
                        {entityRelation}
                        {
                            // TODO: show multiple (array)
                            _.get(this.props, 'rules.typeRules[0].typeUri', undefined) ? (
                                <div
                                    className="ecc-silk-mapping__rulesviewer__targetEntityType"
                                >
                                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                            {
                                                (this.props.rules.typeRules.length > 1) ? 'Target entity types' : 'Target entity type'
                                            }
                                        </dt>
                                        {
                                            this.props.rules.typeRules.map(
                                                function(typeRule) {
                                                    return [
                                                        <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                            <ThingName id={typeRule.typeUri}/>
                                                        </dd>,
                                                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                            <code>{typeRule.typeUri}</code>
                                                        </dd>,
                                                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                            <Info border>
                                                                <ThingDescription id={typeRule.typeUri}/>
                                                            </Info>
                                                        </dd>
                                                    ];
                                                }
                                            )
                                        }
                                    </dl>
                                </div>
                            ) : false
                        }
                        {
                            (this.props.type === 'object') ? (
                                <div
                                    className="ecc-silk-mapping__rulesviewer__sourcePath"
                                >
                                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                            Source path
                                        </dt>
                                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                            <SourcePath
                                                rule={
                                                    {
                                                        type: this.props.type,
                                                        sourcePath: this.props.sourcePath,
                                                    }
                                                }
                                            />
                                        </dd>
                                    </dl>
                                </div>
                            ) : false
                        }
                        {
                            _.get(this.props, 'rules.uriRule.pattern', '') ? (
                                <div
                                    className="ecc-silk-mapping__rulesviewer__idpattern"
                                >
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__comment"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                URI pattern
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                <code>{_.get(this.props, 'rules.uriRule.pattern', '')}</code>
                                            </dd>
                                        </dl>
                                    </div>
                                </div>
                            ) : false
                        }
                        {
                            _.has(this.props, 'metadata.description', false) ? (
                                <div
                                    className="ecc-silk-mapping__rulesviewer__comment"
                                >
                                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                            Description
                                        </dt>
                                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                            {_.get(this.props, 'metadata.description', '')}
                                        </dd>
                                    </dl>
                                </div>
                            ) : false
                        }
                        { _.has(this.props, 'rules.uriRule.id')
                            ? <div
                            className="ecc-silk-mapping__rulesviewer__examples"
                            >
                            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                    Examples of target data
                                </dt>
                                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">

                                    <ExampleView
                                        id={this.props.rules.uriRule.id}
                                    />
                                </dd>
                            </dl>
                        </div>
                        : false}
                    </div>
                    <div className="ecc-silk-mapping__rulesviewer__actionrow mdl-card__actions mdl-card--border">
                        <Button
                            className="ecc-silk-mapping__rulesviewer__actionrow-edit"
                            onClick={this.handleEdit}
                        >
                            Edit
                        </Button>
                        {deleteButton}
                    </div>
                </div>
            </div>
        );
    },

});

export default RuleObjectView;
