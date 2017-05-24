import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button, SelectBox, Radio, RadioGroup, TextField} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';
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
        const {typeRules = [], uriRule = {}} = _.get(this.props, 'rules', {});

        return {
            // TODO: get it from props
            targetProperty: undefined,
            // TODO: get it from props
            targetEntityType: undefined,
            // TODO: get it from props
            entityConnection: 'from',
            pattern: uriRule.pattern,
        };
    },

    handleConfirm() {
        hierarchicalMappingChannel.subject('ruleId.createMapping').onNext({
            // if id is undefined -> we are creating a new rule
            id: this.props.id,
            type: this.props.type,
            targetProperty: this.state.targetProperty,
            targetEntityType: this.state.targetEntityType,
            pattern: this.state.pattern,
            entityConnection: this.state.entityConnection,
        });
        this.props.onClose();
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
    handleEdit(event) {
        hierarchicalMappingChannel.subject('ruleId.edit').onNext({ruleId: this.props.id});
        event.stopPropagation();
    },
    // remove rule
    handleRemove(event) {
        console.log('click remove');
        // TODO: add remove event
        event.stopPropagation();
    },

    // template rendering
    render () {
        const {
            id,
            edit,
        } = this.props;

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm = !(this.state.targetProperty && this.state.targetEntityType);

        console.warn('debug OBJECT edit view', this.props);

        const title = (
            // TODO: add source path if: parent, not edit, not root element
            edit && !id ? (
                <div className="mdl-card__title">
                    Add object mapping
                </div>
            ) : false
        );

        const targetPropertyInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose target property'}
                    className="ecc-component-hierarchicalMapping__content-editView-object__content__targetProperty"
                    // TODO: get list of target properties
                    options={[]}
                    value={this.state.targetProperty}
                    onChange={this.handleChangeSelectBox.bind(null, 'targetProperty')}
                />
            ) : (
                <div
                    className="ecc-component-hierarchicalMapping__content-editView-object__content__targetProperty"
                >
                    Target property
                    {this.state.targetProperty}
                </div>
            )
        );

        const entityRelationInput = (
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

        const targetEntityTypeInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose target entity type'}
                    className="ecc-component-hierarchicalMapping__content-editView-object__content__targetEntityType"
                    // TODO: get list of target entity types
                    options={[]}
                    value={this.state.targetEntityType}
                    onChange={this.handleChangeSelectBox.bind(null, 'targetEntityType')}
                />
            ) : (
                <div
                    className="ecc-component-hierarchicalMapping__content-editView-object__content__targetEntityType"
                >
                    Target entity type
                    {this.state.targetEntityType}
                </div>
            )
        );

        const pattern = (
            id ? (
                <div>
                    Id pattern<br/>
                    <TextField
                        label="Id pattern"
                        className="ecc-component-hierarchicalMapping__content-editView-object__content__pattern"
                        value={this.state.pattern}
                        onChange={this.handleChangeTextfield.bind(null, 'pattern')}
                        disabled={!edit}
                    />
                </div>
            ) : false
        );

        const actionRow = (
            edit ? (
                <div className="ecc-component-hierarchicalMapping__content-editView-object__actionrow">
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-object__actionrow-save"
                        onClick={this.handleConfirm}
                        disabled={allowConfirm}
                    >
                        Save
                    </Button>
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-object__actionrow-cancel"
                        onClick={this.props.onClose}
                    >
                        Cancel
                    </Button>
                </div>
            ) : (
                <div className="ecc-component-hierarchicalMapping__content-editView-object__actionrow">
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-object__actionrow-edit"
                        onClick={this.handleEdit}
                    >
                        Edit
                    </Button>
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-object__actionrow-remove"
                        onClick={this.handleRemove}
                        disabled
                    >
                        Remove (TODO)
                    </Button>
                </div>
            )
        );

        return (
            <div
                className="ecc-component-hierarchicalMapping__content-editView-object"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch stretch-vertical">
                    {title}
                    <div className="mdl-card__content">
                        {targetPropertyInput}
                        {entityRelationInput}
                        {targetEntityTypeInput}
                        {pattern}
                        {actionRow}
                        {
                            // TODO: if not in edit mode user should see modified and creator
                            // store data not exist at the moment - mockup for now?
                        }
                    </div>
                </div>
            </div>
        );
    },

});

export default RuleObjectEditView;