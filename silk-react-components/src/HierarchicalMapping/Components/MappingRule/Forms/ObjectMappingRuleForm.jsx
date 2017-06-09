import React from 'react';
import UseMessageBus from '../../../UseMessageBusMixin';
import {
    SelectBox,
    Radio,
    RadioGroup,
    TextField,
    AffirmativeButton,
    DismissiveButton,
} from 'ecc-gui-elements';
import {ThingClassName} from '../SharedComponents';
import hierarchicalMappingChannel from '../../../store';
import _ from 'lodash';

const ObjectMappingRuleForm = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        id: React.PropTypes.string,
        onClose: React.PropTypes.func.isRequired,
    },
    getInitialState() {
        return {
            loading: true,
        };
    },
    componentDidMount(){
        this.loadData();
    },
    loadData(){
        if (this.props.id) {
            hierarchicalMappingChannel.request(
                {
                    topic: 'rule.get',
                    data: {
                        id: this.props.id,
                    }
                }
            )
                .subscribe(
                    ({rule}) => {
                        this.setState({
                            loading: false,
                            targetProperty: _.get(rule, 'mappingTarget.uri', undefined),
                            sourceProperty: _.get(rule, 'sourcePath', undefined),
                            comment: _.get(rule, 'metadata.description', ''),
                            targetEntityType: _.get(rule, 'rules.typeRules[0].typeUri', undefined),
                            entityConnection: _.get(rule, 'mappingTarget.inverse', false) ? 'to' : 'from',
                            pattern: _.get(rule, 'rules.uriRule.pattern', ''),
                        });
                    },
                    (err) => {
                        console.warn('err MappingRuleOverview: rule.get');
                        this.setState({loading: false});
                    }
                );
        } else {
            this.setState({
                loading: false,
                type: 'direct',
            })
        }
    },
    handleConfirm() {

        hierarchicalMappingChannel.subject('rule.createObjectMapping').onNext({
            id: this.props.id,
            parentId: this.props.parentId,
            type: this.props.type,
            comment: this.state.comment,
            sourcePath: this.state.sourceProperty,
            targetProperty: this.state.targetProperty,
            targetEntityType: this.state.targetEntityType,
            pattern: this.state.pattern,
            entityConnection: this.state.entityConnection === 'to',
        });

        this.handleClose(null);
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
    handleClose(event) {
        //event.stopPropagation();
        if (_.isFunction(this.props.onClose)) {
            this.props.onClose();
        } else {
            console.warn('ValueMappingRuleForm: No onClose')
        }
        hierarchicalMappingChannel.subject('ruleView.closed').onNext({id: this.props.id});
    },
    // template rendering
    render () {
        const {
            id,
            type,
        } = this.props;

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm = type === 'root'
            ? true
            : this.state.targetProperty;

        const title = (
            // TODO: add source path if: parent, not edit, not root element
            !id
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
                (
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
                )
            );

            entityRelationInput = (
                <RadioGroup
                    onChange={this.handleChangeRadio.bind(null, 'entityConnection')}
                    value={this.state.entityConnection}
                    name=""
                    disabled={false}
                >
                    <Radio
                        value="from"
                        label={<div>Connects from {<ThingClassName id={this.props.parentId} name={this.props.parentName}/>}</div>}
                    />
                    <Radio
                        value="to"
                        label={<div>Connects to {<ThingClassName id={this.props.parentId} name={this.props.parentName}/>}</div>}
                    />
                </RadioGroup>
            );

        }

        let patternInput = false;

        if (id) {
            patternInput = (
                (
                    <TextField
                        label="Id pattern"
                        className="ecc-silk-mapping__ruleseditor__pattern"
                        value={this.state.pattern}
                        onChange={this.handleChangeTextfield.bind(null, 'pattern')}
                    />
                )
            );
        }

        // FIXME: created and updated need to be formated. Creator is not available in Dataintegration :(

        return (
            (
                <div
                    className="ecc-silk-mapping__ruleseditor"
                >
                    <div className={
                        "mdl-card mdl-card--stretch" +
                        (!id ? ' mdl-shadow--2dp' : '')
                    }>
                        {title}
                        <div className="mdl-card__content">
                            {targetPropertyInput}
                            {entityRelationInput}
                            <SelectBox
                                placeholder={'Choose target entity type'}
                                className={'ecc-silk-mapping__ruleseditor__targetEntityType'}
                                options={['foaf:Person', 'schema:Country', 'schema:Address']}
                                value={this.state.targetEntityType}
                                //multi={true} // allow multi selection
                                onChange={this.handleChangeSelectBox.bind(null, 'targetEntityType')}
                            />
                            <TextField
                                label={'Source property'}
                                onChange={this.handleChangeTextfield.bind(null, 'sourceProperty')}
                                value={this.state.sourceProperty}
                            />
                            <TextField
                                multiline={true}
                                label="Comment"
                                className="ecc-silk-mapping__ruleseditor__comment"
                                value={this.state.comment}
                                onChange={this.handleChangeTextfield.bind(null, 'comment')}
                            />
                            {patternInput}
                        </div>
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
                    </div>
                </div>
            )
        );
    },

});

export default ObjectMappingRuleForm;
