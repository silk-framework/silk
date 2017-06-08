import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Button,
    Radio,
    RadioGroup,
    ConfirmationDialog,
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
    Info,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';
import ObjectMappingRuleForm from './Forms/ObjectMappingRuleForm';
import {
    SourcePath,
    ThingName,
    ThingDescription,
} from './SharedComponents';

const RuleObjectEditView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        parent: React.PropTypes.string.isRequired,
        type: React.PropTypes.string,
        rules: React.PropTypes.object,
        onClose: React.PropTypes.func,
        edit: React.PropTypes.bool.isRequired,
    },

    getInitialState() {
        return {
            edit: !!this.props.edit,
        };
    },


    // open view in edit mode
    handleEdit() {
        hierarchicalMappingChannel.subject('ruleView.edit').onNext({id: this.props.id});
        this.setState({
            edit: !this.state.edit,
        })
    },

    handleComplexEdit(event) {
        event.stopPropagation();
        alert('Normally this would open the complex editor (aka jsplumb view)')
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
                parentId={this.props.parentId}
                onClose={() => this.setState({edit: false}) }
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
                                <ThingName id={_.get(this.props, 'mappingTarget.uri', undefined)} />
                            </dd>
                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                <code>{_.get(this.props, 'mappingTarget.uri', undefined)}</code>
                            </dd>
                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                <Info border>
                                    <ThingDescription id={_.get(this.props, 'mappingTarget.uri', undefined)} />
                                </Info>
                            </dd>
                        </dl>
                    </div>
                )
            );

            entityRelation = (
                <RadioGroup
                    value={_.get(this.props, 'mappingTarget.inverse', false) ? 'to' : 'from'}
                    name=""
                    disabled={true}
                >
                    <Radio
                        value="from"
                        label="Connects from entity"
                    />
                    <Radio
                        value="to"
                        label="Connects to entity"
                    />
                </RadioGroup>
            );


            deleteButton = (
                <DisruptiveButton
                    className="ecc-silk-mapping__rulesviewer__actionrow-remove"
                    onClick={()=>hierarchicalMappingChannel.subject('removeClick').onNext({id: this.props.id, type: this.props.type, parent: this.props.parent})}
                >
                    Remove
                </DisruptiveButton>
            );

        }

        // TODO: Move up

        return (
            (
                <div>
                    <div className="mdl-card__content">
                        <div
                            className="ecc-silk-mapping__rulesviewer"
                        >
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
                                                                <ThingName id={typeRule.typeUri} />
                                                            </dd>,
                                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                                <code>{typeRule.typeUri}</code>
                                                            </dd>,
                                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                                <Info border>
                                                                    <ThingDescription id={typeRule.typeUri} />
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
                                (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__sourcePath"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Source property
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                TODO: What is the source path of a object mapping?
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
                                )
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
                                                    Identifier pattern
                                                </dt>
                                                <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                    <code>{_.get(this.props, 'rules.uriRule.pattern', '')}</code>
                                                </dd>
                                                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                    TODO: complex pattern example?
                                                </dd>
                                                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                    <Button
                                                        className="ecc-silk-mapping__rulesviewer__actionrow-complex-edit"
                                                        onClick={this.handleComplexEdit}
                                                        raised
                                                    >
                                                        {
                                                            _.isArray(_.get(this.props, 'rules.uriRule.pattern', '')) ? 'Edit complex mapping' : 'Create complex mapping'
                                                        }
                                                    </Button>
                                                </dd>
                                            </dl>
                                        </div>
                                    </div>
                                ) : false
                            }
                            {
                                _.get(this.props, 'metadata.description', '') ? (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__comment"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Comment
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                {_.get(this.props, 'metadata.description', '')}
                                            </dd>
                                        </dl>
                                    </div>
                                ) : false
                            }
                            <div
                                className="ecc-silk-mapping__rulesviewer__examples"
                            >
                                <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                    <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                        Examples of target data
                                    </dt>
                                    <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                        TODO
                                    </dd>
                                </dl>
                            </div>
                        </div>
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
            )
        );
    },

});

export default RuleObjectEditView;
