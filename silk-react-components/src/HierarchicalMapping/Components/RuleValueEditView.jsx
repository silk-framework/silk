import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button, TextField, SelectBox} from 'ecc-gui-elements';

import hierarchicalMappingChannel from '../store';

const RuleValueEditView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        name: React.PropTypes.string,
        //operator: React.PropTypes.object,
        type: React.PropTypes.string,
        // FIXME: sourcePath === source property?
        sourcePath: React.PropTypes.string,
        // FIXME: targetProperty === mappingTarget?
        targetProperty: React.PropTypes.string,
        mappingTarget: React.PropTypes.object,
        onClose: React.PropTypes.func.isRequired,
    },

    getInitialState() {
        return {
            comment: this.props.comment || '',
            targetProperty: this.props.targetProperty,
            // FIXME: get it from props in edit mode
            propertyType: undefined,
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

    // template rendering
    render () {
        const {
            id ,
        } = this.props;

        console.warn('debug VALUE edit view', this.props);

        return (
            <div
                className="ecc-component-hierarchicalMapping__content-editView-value"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch stretch-vertical">
                    <div
                        className="mdl-card__title"
                    >
                        {id ? 'Edit' : 'Add'} value mapping
                    </div>
                    <div className="mdl-card__content">
                        <SelectBox
                            placeholder={'Choose target property'}
                            className="ecc-component-hierarchicalMapping__content-editView-value__content__targetProperty"
                            // TODO: get list of target properties
                            options={[]}
                            value={this.state.targetProperty}
                            onChange={this.handleChangeSelectBox.bind(null, 'targetProperty')}
                        />
                        <SelectBox
                            placeholder={'Choose property type'}
                            className="ecc-component-hierarchicalMapping__content-editView-value__content__propertyType"
                            // TODO: get list of property types
                            options={[]}
                            value={this.state.propertyType}
                            onChange={this.handleChangeSelectBox.bind(null, 'propertyType')}
                        />
                        {/*TODO: Which gui element to use?*/}
                        Source property<br/>
                        {this.state.sourceProperty}
                        <TextField
                            multiline={true}
                            label="Comment"
                            className="ecc-component-hierarchicalMapping__content-editView-value__content__comment"
                            value={this.state.comment}
                            onChange={this.handleChangeTextfield.bind(null, 'comment')}
                        />
                        <div className="ecc-component-hierarchicalMapping__content-editView-value__actionrow">
                            <Button
                                className="ecc-component-hierarchicalMapping__content-editView-value__actionrow-save"
                                onClick={this.handleConfirm}
                                disabled
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
                    </div>
                </div>
            </div>
        );
    },

});

export default RuleValueEditView;