import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
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
        hierarchicalMappingChannel.subject('rule.createValueMapping').onNext({
            // if id is undefined -> we are creating a new rule
            id: this.props.id,
            parentId: this.props.parentId,
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
    handleComplexEdit() {
        alert('Normally this would open the complex editor (aka jsplumb view)')
    },
    // open view in edit mode
    handleEdit(event) {
        hierarchicalMappingChannel.subject('ruleId.edit').onNext({ruleId: this.props.id});
        event.stopPropagation();
    },
    // remove rule
    handleRemove(event) {
        console.log('click remove');
    },
    // template rendering
    render () {
        const {
            id,
            edit,
            type = 'direct',
        } = this.props;


        // FIXME: also check if data really has changed before allow saving
        const allowConfirm = !(this.state.targetProperty && this.state.propertyType);

        const title = (
            edit && !id ? (
                <div className="mdl-card__title">
                    Add value mapping
                </div>
            ) : false
        );

        //TODO: Where to get the list of target Properties?
        const targetPropertyInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose target property'}
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__targetProperty"
                    options={[
                        'http://xmlns.com/foaf/0.1/name',
                        'http://xmlns.com/foaf/0.1/knows',
                        'http://xmlns.com/foaf/0.1/familyName',
                    ]}
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

        //TODO: Where to get the list of target property types?
        const propertyTypeInput = (
            edit ? (
                <SelectBox
                    placeholder={'Choose property type'}
                    className="ecc-component-hierarchicalMapping__content-editView-value__content__propertyType"
                    options={[
                        'AutoDetectValueType',
                        'StringValueType',
                        'UriValueType',
                    ]}
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

        //TODO: Unfold complex mapping
        let sourceProperty = edit ? false : (
            <div>
                Source property
                {this.state.sourceProperty || 'Complex Mapping'} <Button onClick={this.handleComplexEdit}>Edit</Button>
            </div>
        );


        console.warn(type);

        if (edit && type === 'direct') {
            sourceProperty = (
                <TextField
                    label={'Source property'}
                    onChange={this.handleChangeTextfield.bind(null, 'sourceProperty')}
                    value={this.state.sourceProperty}
                />
            );
        } else if (edit && type === 'complex') {
            sourceProperty = (
                <TextField
                    disabled={true}
                    label="Source property"
                    value="Complex Mapping"
                />
            )
        }

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
                        {sourceProperty}
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
