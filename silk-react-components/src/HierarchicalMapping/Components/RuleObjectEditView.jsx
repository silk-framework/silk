import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
import {
    Button,
    SelectBox,
    Radio,
    RadioGroup,
    TextField,
    ConfirmationDialog,
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';
import {Rx} from 'ecc-messagebus';
import _ from 'lodash';

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
            targetProperty: _.get(this.props, 'mappingTarget.uri', undefined),
            comment: this.props.comment,
            targetEntityType: _.get(this.props, 'rules.typeRules[0].typeUri', undefined),
            entityConnection: _.get(this.props, 'mappingTarget.inverse', false) ? 'to' : 'from',
            pattern: _.get(this.props, 'rules.uriRule.pattern', ''),
            edit: !!this.props.edit,
        };
    },

    handleClickRemove(event) {
        event.stopPropagation();
        this.setState({
            elementToDelete: this.props.id,
        });
    },
    handleConfirm() {
        hierarchicalMappingChannel.subject('rule.createObjectMapping').onNext({
            // if id is undefined -> we are creating a new rule
            id: this.props.id,
            parentId: this.props.parentId,
            type: this.props.type,
            comment: this.state.comment,
            targetProperty: this.state.targetProperty,
            targetEntityType: this.state.targetEntityType,
            pattern: this.state.pattern,
            entityConnection: this.state.entityConnection === 'to',
        });
        this.handleClose();
    },

    handleChangeSelectBox(state, value) {
        this.setState({
            [state]: value,
        });
    },
    handleChangeTextfield(state, {value}) {
        this.setState({
            [state]: value,
        });
    },
    handleChangeRadio(state, {value}) {
        this.setState({
            [state]: value,
        });
    },

    // open view in edit mode
    handleEdit() {
        this.setState({
            edit: !this.state.edit,
        })
    },
    handleClose() {
        if (_.isFunction(this.props.onClose)) {
            this.props.onClose();
        } else {
            this.setState({
                edit: false,
            })
        }
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
        this.setState({
            elementToDelete: false,
        });
    },

    // template rendering
    render () {
        const {
            id,
            type,
        } = this.props;
        const {edit} = this.state;

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm =  type === 'root'
            ? true
            : this.state.targetProperty && this.state.targetEntityType;

        console.warn('debug OBJECT edit view', this.props);

        const title = (
            // TODO: add source path if: parent, not edit, not root element
            edit && !id
                ? (
                <div className="mdl-card__title mdl-card--border">
                    Add object mapping
                </div>
                )
                : false
        );

        let targetPropertyInput = false;
        let entityRelationInput = false;

        if (type !== 'root') {
            // TODO: where to get get list of target properties
            targetPropertyInput = (
                edit ? (
                    <SelectBox
                        placeholder={'Choose target property'}
                        className="ecc-silk-mapping__ruleseditor__targetProperty"
                        options={[
                            'target:address',
                            'target:country',
                            'target:friend',
                        ]}
                        value={this.state.targetProperty}
                        onChange={this.handleChangeSelectBox.bind(null, 'targetProperty')}
                    />
                ) : (
                    <div
                        className="ecc-silk-mapping__ruleseditor__targetProperty"
                    >
                        Target property
                        {this.state.targetProperty}
                    </div>
                )
            );

            entityRelationInput = (
                <RadioGroup
                    onChange={this.handleChangeRadio.bind(null, 'entityConnection')}
                    value={this.state.entityConnection}
                    name=""
                    disabled={!edit}
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

        }

        // TODO: where to get get list of target entities
        const targetEntityTypeInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose target entity type'}
                    className="ecc-silk-mapping__ruleseditor__targetEntityType"
                    options={['foaf:Person', 'schema:Country', 'schema:Address']}
                    value={this.state.targetEntityType}
                    onChange={this.handleChangeSelectBox.bind(null, 'targetEntityType')}
                />
            ) : (
                <div
                    className="ecc-silk-mapping__ruleseditor__targetEntityType"
                >
                    Target entity type
                    {this.state.targetEntityType}
                </div>
            )
        );

        const commentInput = (
            edit ? (
                <TextField
                    multiline={true}
                    label="Comment"
                    className="ecc-silk-mapping__ruleseditor__comment"
                    value={this.state.comment}
                    onChange={this.handleChangeTextfield.bind(null, 'comment')}
                />
            ) : (
                <div
                    className="ecc-silk-mapping__ruleseditor__comment"
                >
                    Comment
                    {this.state.comment}
                </div>
            )
        );

        let pattern = false;

        if (id) {
            pattern = (
                edit ? (
                    <TextField
                        label="Id pattern"
                        className="ecc-silk-mapping__ruleseditor__pattern"
                        value={this.state.pattern}
                        onChange={this.handleChangeTextfield.bind(null, 'pattern')}
                    />
                ) : (
                    <div
                        className="ecc-silk-mapping__ruleseditor__pattern"
                    >
                        Id pattern
                        {this.state.pattern}
                    </div>
                )
            );
        }

        const actionRow = (
            edit ? (
                <div className="ecc-silk-mapping__ruleseditor__actionrow mdl-card__actions mdl-card--border">
                    <AffirmativeButton
                        className="ecc-silk-mapping__ruleseditor__actionrow-save"
                        onClick={this.handleConfirm}
                        disabled={!allowConfirm}
                    >
                        Save
                    </AffirmativeButton>
                    <DismissiveButton
                        className="ecc-silk-mapping__ruleseditor__actionrow-cancel"
                        onClick={this.handleClose}
                    >
                        Cancel
                    </DismissiveButton>
                </div>
            ) : (
                <div className="ecc-silk-mapping__ruleseditor__actionrow mdl-card__actions mdl-card--border">
                    <Button
                        className="ecc-silk-mapping__ruleseditor__actionrow-edit"
                        onClick={this.handleEdit}
                    >
                        Edit
                    </Button>
                    <DisruptiveButton
                        className="ecc-silk-mapping__ruleseditor__actionrow-remove"
                        onClick={this.handleClickRemove}
                        disabled={type === 'root'}
                    >
                        Remove
                    </DisruptiveButton>
                </div>
            )
        );

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

        // FIXME: EditView should not mix View and Edit functionality
        return (
            edit ? (
                <div
                    className="ecc-silk-mapping__ruleseditor"
                >
                    <div className="mdl-card mdl-shadow--2dp mdl-card--stretch">
                        {title}
                        <div className="mdl-card__content">
                            {targetPropertyInput}
                            {entityRelationInput}
                            {targetEntityTypeInput}
                            {commentInput}
                            {pattern}
                            {
                                // TODO: if not in edit mode user should see modified and creator
                                // store data not exist at the moment - mockup for now?
                            }
                        </div>
                        {actionRow}
                    </div>
                </div>
            ) : (
                <div>
                    {deleteView}
                    <div className="mdl-card__content">
                        {targetPropertyInput}
                        {entityRelationInput}
                        {targetEntityTypeInput}
                        {commentInput}
                        {pattern}
                        {
                            // TODO: if not in edit mode user should see modified and creator
                            // store data not exist at the moment - mockup for now?
                        }
                    </div>
                    {actionRow}
                </div>
            )
        );
    },

});

export default RuleObjectEditView;
