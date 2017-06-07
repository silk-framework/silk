import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Button,
    ConfirmationDialog,
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';
import ValueMappingRuleForm from './Forms/ValueMappingRuleForm';

const RuleValueEditView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        //operator: React.PropTypes.object,
        type: React.PropTypes.string,
        // FIXME: sourcePath === source property?
        sourcePath: React.PropTypes.string,
        mappingTarget: React.PropTypes.object,
        onClose: React.PropTypes.func,
        edit: React.PropTypes.bool.isRequired,
        handleToggleExpand: React.PropTypes.func,
    },

    getInitialState() {
        return {
            edit: this.props.edit,
        };
    },
    // remove rule
    handleClickRemove(event) {
        event.stopPropagation();
        this.setState({
            elementToDelete: this.props.id,
        });
    },
    handleComplexEdit(event) {
        event.stopPropagation();
        alert('Normally this would open the complex editor (aka jsplumb view)')
    },
    // open view in edit mode
    handleEdit(event) {
        event.stopPropagation();
        hierarchicalMappingChannel.subject('ruleView.edit').onNext({id: this.props.id});
        this.setState({
            edit: !this.state.edit,
        })
    },
    handleClose(event) {
        event.stopPropagation();
        if (_.isFunction(this.props.onClose)) {
            this.props.onClose();
        } else {
            this.setState({
                edit: false,
            })
        }
        hierarchicalMappingChannel.subject('ruleView.closed').onNext({id: this.props.id});
    },
    handleConfirmRemove(event) {
        event.stopPropagation();
        hierarchicalMappingChannel.request({topic: 'rule.removeRule', data: {id: this.state.elementToDelete}})
            .subscribe(
                () => {
                    // FIXME: let know the user which element is gone!
                    this.setState({
                        elementToDelete: false,
                    });
                },
                (err) => {
                    // FIXME: let know the user what have happened!
                    this.setState({
                        elementToDelete: false,
                    });
                }
            );
    },
    handleCancelRemove() {
        event.stopPropagation();
        this.setState({
            elementToDelete: false,
        });
    },
    // template rendering
    render () {
        const {edit} = this.state;

        if (edit) {
            return <ValueMappingRuleForm
                {
                    // Fixme: Remove once we load data directly in form
                    ...this.props
                }
                id={this.props.id}
                parentId={this.props.parentId}
                onClose={() => this.setState({edit: false}) }
            />
        }

        //TODO: Move delete view out of here!
        const deleteView = this.state.elementToDelete
            ? <ConfirmationDialog
                active={true}
                title="Delete Rule"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleConfirmRemove}>
                        Delete
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelRemove}>
                        Cancel
                    </DismissiveButton>
                }>
                Are you sure you want to delete the rule with id '{this.state.elementToDelete}'?
            </ConfirmationDialog>
            : false;

        // FIXME: created and updated need to be formated. Creator is not available in Dataintegration :(

        return (
            (
                <div
                    className="ecc-silk-mapping__rulesview"
                >
                    {deleteView}
                    <div className="mdl-card mdl-card--stretch">
                        <div
                            className="ecc-silk-mapping__rulesviewer__title mdl-card__title mdl-card--border clickable"
                            onClick={this.props.handleToggleExpand}
                        >
                            <div className="mdl-card__title-text">
                                {_.get(this.props, 'mappingTarget.uri', undefined)}
                            </div>
                        </div>
                        <div className="mdl-card__content">
                            <div
                                className="ecc-silk-mapping__rulesviewer__targetProperty"
                            >
                                Target property
                                {_.get(this.props, 'mappingTarget.uri', undefined)}
                            </div>
                            <div
                                className="ecc-silk-mapping__rulesviewer__propertyType"
                            >
                                Property type
                                {_.get(this.props, 'mappingTarget.valueType.nodeType', undefined)}
                            </div>
                            <div>
                                Source property
                                {this.props.sourcePath || 'Complex Mapping'}
                            </div>
                            <div
                                className="ecc-silk-mapping__rulesviewer__comment"
                            >
                                Comment
                                {this.props.comment || ''}
                            </div>

                            <div className="ecc-silk-mapping__rulesviewer__created">
                                Created {this.props.created ? this.props.created : 0}</div>
                            <div className="ecc-silk-mapping__rulesviewer__updated">
                                Updated {this.props.updated ? this.props.updated : 0}</div>
                        </div>
                        <div className="ecc-silk-mapping__rulesviewer__actionrow mdl-card__actions mdl-card--border">
                            <Button
                                className="ecc-silk-mapping__rulesviewer__actionrow-edit"
                                onClick={this.handleEdit}
                            >
                                Edit rule
                            </Button>
                            <Button
                                className="ecc-silk-mapping__rulesviewer__actionrow-complex-edit"
                                onClick={this.handleComplexEdit}
                            >
                                Edit complex
                            </Button>
                            <DisruptiveButton
                                className="ecc-silk-mapping__rulesviewer__actionrow-remove"
                                onClick={this.handleClickRemove}
                                disabled={false} // FIXME: all elements are removable?
                            >
                                Remove rule
                            </DisruptiveButton>
                        </div>
                    </div>
                </div>
            )
        );
    },

});

export default RuleValueEditView;
