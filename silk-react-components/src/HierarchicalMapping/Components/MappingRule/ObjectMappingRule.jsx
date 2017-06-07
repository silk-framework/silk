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
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';
import ObjectMappingRuleForm from './Forms/ObjectMappingRuleForm';
import {SourcePath} from './SharedComponents';

const RuleObjectEditView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
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
                {
                    // Fixme: Remove once we load data directly in form
                    ...this.props
                }
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
                        className="ecc-silk-mapping__ruleseditor__targetProperty"
                    >
                        Target property
                        {_.get(this.props, 'mappingTarget.uri', undefined)}
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
                    className="ecc-silk-mapping__ruleseditor__actionrow-remove"
                    onClick={()=>hierarchicalMappingChannel.subject('removeClick').onNext({id: this.props.id, type: this.props.type})}
                >
                    Remove rule
                </DisruptiveButton>
            );

        }

        // TODO: Move up

        return (
            (
                <div>
                    <div className="mdl-card__content">
                        {targetProperty}
                        {entityRelation}
                        <div
                            className="ecc-silk-mapping__ruleseditor__targetEntityType"
                        >
                            Target entity type
                            {_.get(this.props, 'rules.typeRules[0].typeUri', undefined)}
                        </div>
                        <div>
                            Source property
                            <SourcePath
                                rule={
                                    {
                                        type: this.props.type,
                                        sourcePath: this.props.sourcePath,
                                    }
                                }
                            />
                        </div>
                        <div
                            className="ecc-silk-mapping__ruleseditor__comment"
                        >
                            Comment
                            {_.get(this.props, 'metadata.description', '')}
                        </div>
                        <div
                            className="ecc-silk-mapping__ruleseditor__pattern"
                        >
                            Id pattern
                            {_.get(this.props, 'rules.uriRule.pattern', '')}
                        </div>
                    </div>
                    <div className="ecc-silk-mapping__ruleseditor__actionrow mdl-card__actions mdl-card--border">
                        <Button
                            className="ecc-silk-mapping__ruleseditor__actionrow-edit"
                            onClick={this.handleEdit}
                        >
                            Edit rule
                        </Button>
                        {deleteButton}
                        <Button
                            className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
                            onClick={this.handleComplexEdit}
                        >
                            Edit complex
                        </Button>
                    </div>
                </div>
            )
        );
    },

});

export default RuleObjectEditView;
