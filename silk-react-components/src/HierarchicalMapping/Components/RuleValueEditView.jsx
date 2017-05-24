import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button, TextField, SelectBox} from 'ecc-gui-elements';
import _ from 'lodash';

import hierarchicalMappingChannel from '../store';

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
    },

    getInitialState() {
        return {
            comment: this.props.comment || '',
            targetProperty: _.get(this.props, 'mappingTarget.uri', undefined),
            propertyType: _.get(this.props, 'mappingTarget.valueType.nodeType', undefined),
            // FIXME: is this editable?
            sourceProperty: this.props.sourcePath,
        };
    },

    handleConfirm() {
        hierarchicalMappingChannel.subject('ruleId.createMapping').onNext({
            // if id is undefined -> we are creating a new rule
            id: this.props.id,
            type: this.props.type,
            comment: this.state.comment,
            targetProperty: this.state.targetProperty,
            propertyType: this.state.propertyType,
            sourceProperty: this.state.sourceProperty,
        });
        this.props.onClose();
    },

    handleChangeTextfield(state, {value}) {
        this.setState({
            [state]: value,
        });
    },
    handleChangeSelectBox(state, value) {
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
            id ,
            edit,
        } = this.props;

        console.warn('debug VALUE edit view', this.props);

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm = !(this.state.targetProperty && this.state.propertyType);

        const title = (
            edit && !id ? (
                <div className="mdl-card__title">
                    Add value mapping
                </div>
            ) : false
        );

        const targetPropertyInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose target property'}
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__targetProperty"
                    // TODO: get list of target properties
                    options={[]}
                    value={this.state.targetProperty}
                    onChange={this.handleChangeSelectBox.bind(null, 'targetProperty')}
                />
            ) : (
                <div
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__targetProperty"
                >
                    Target property
                    {this.state.targetProperty}
                </div>
            )
        );

        const propertyTypeInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose property type'}
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__propertyType"
                    // TODO: get list of property types
                    options={[]}
                    value={this.state.propertyType}
                    onChange={this.handleChangeSelectBox.bind(null, 'propertyType')}
                />
            ) : (
                <div
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__propertyType"
                >
                    Property type
                    {this.state.propertyType}
                </div>
            )
        );

        const commentInput = (
            edit ? (
                <TextField
                    multiline={true}
                    label="Comment"
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__comment"
                    value={this.state.comment}
                    onChange={this.handleChangeTextfield.bind(null, 'comment')}
                />
            ) : (
                <div
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__comment"
                >
                    Comment
                    {this.state.comment}
                </div>
            )
        );

        const actionRow = (
            edit ? (
                <div className="ecc-component-hierarchicalMapping__content-editView-value__actionrow">
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-value__actionrow-save"
                        onClick={this.handleConfirm}
                        disabled={allowConfirm}
                    >
                        Save
                    </Button>
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-value__actionrow-cancel"
                        onClick={this.props.onClose}
                    >
                        Cancel
                    </Button>
                </div>
            ) : (
                <div className="ecc-component-hierarchicalMapping__content-editView-value__actionrow">
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-value__actionrow-edit"
                        onClick={this.handleEdit}
                    >
                        Edit
                    </Button>
                    <Button
                        className="ecc-component-hierarchicalMapping__content-editView-value__actionrow-remove"
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
                className="ecc-component-hierarchicalMapping__content-editView-value"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch stretch-vertical">
                    {title}
                    <div className="mdl-card__content">
                        {targetPropertyInput}
                        {propertyTypeInput}
                        {/*TODO: Which gui element to use?*/}
                        Source property<br/>
                        {this.state.sourceProperty}
                        {commentInput}
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

export default RuleValueEditView;