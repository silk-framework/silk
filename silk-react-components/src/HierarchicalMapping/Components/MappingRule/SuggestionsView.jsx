import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Spinner,
    Error,
    Checkbox,
    Button,
    AffirmativeButton,
    DismissiveButton,
    ContextMenu,
    MenuItem,
    Chip,
} from 'ecc-gui-elements';
import SuggestionView from './SuggestionView';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';

let pendingRules = {};
let wrongRules = {};
const SuggestionsView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        targetClassUris: React.PropTypes.array,

    },
    check(value, id, event) {
        const i = `${value};${id}`;
        this.setState({
            checked: _.includes(this.state.checked, i)
                ? _.filter(this.state.checked, (v) => v !== i)
                : _.concat(this.state.checked, [i])
        })
    },
    isChecked(key,i){
        return _.includes(this.state.checked,`${key};${i}`)
    },
    loadData(){
        this.setState({
            loading: true,
        });
        pendingRules = {};
        wrongRules = {}
        hierarchicalMappingChannel.request(
            {
                topic: 'rule.suggestions',
                data: {
                    targetClassUris: this.props.targetClassUris,
                    ruleId: this.props.ruleId,
                }
            }).subscribe(
            (response) => {
                this.setState({
                    loading: false,
                    data: response.suggestions,
                });
            },
            (err) => {
                console.warn('err MappingRuleOverview: rule.suggestions');
                this.setState({loading: false});
            },
        );

    },
    componentDidMount() {
        this.loadData();
    },
    handleAddSuggestions(event) {
        event.stopPropagation();
        let correspondences = [];
        console.log(this.state.data)
        this.setState({
            loading: true,
        })
        _.map(this.state.checked, (suggestion) => {
            const a = _.split(suggestion, ';');
            correspondences.push({
                sourcePath: this.state.data[a[0]][a[1]].uri,
                targetProperty: a[0],
            });
        });
        hierarchicalMappingChannel.request(
        {
            topic: 'rules.generate',
            data: {
                correspondences,
                parentRuleId: this.props.ruleId,
            }
        }).subscribe(

            (response) => {
                this.setState({loading: true});
                _.map(response.rules, (rule, k) => {
                    this.saveRule({...rule, parentId: this.props.ruleId}, k);
                });
                hierarchicalMappingChannel.subject('reload').onNext(true);
            },
            (err) => {
                console.warn('err MappingRuleOverview: rule.suggestions');
                this.setState({loading: false});
            }
        );
    },
    saveRule(rule, pos) {
        rule.id = undefined;
        pendingRules[pos]= true;
        hierarchicalMappingChannel.request({
            topic: 'rule.createGeneratedMapping',
            data: {...rule},
        }).subscribe(
            () => {
                this.onSafeDone(pos);
            },
            (err) => {
                wrongRules[pos] = {
                    msg: err,
                    rule: rule,
                };
                this.onSafeDone(pos);
            },
            () => {
            }
        );
    },
    onSafeDone(pos) {
        delete pendingRules[pos];
        if (_.size(pendingRules) === 0) {
            if (_.size(wrongRules) > 0) {
                this.setState({
                    loading: false,
                    error: wrongRules,
                })
            }
            else {
                hierarchicalMappingChannel.subject('ruleView.close').onNext({id:0});
                this.props.onClose();
            }
        }
    },
    getInitialState() {
        return {
            data: undefined,
            checked: [],
            error: false,
        };
    },
    checkAll(event) {
        let checked = [];
        event.stopPropagation();
        _.map(this.state.data, (value, key) => {
            _.map(value, (e,i) => {
                checked.push(`${key};${i}`);
            })
        })
        this.setState({checked});
    },
    checkNone(event) {
        event.stopPropagation();
        this.setState({
            checked: false,
        });
    },
    // template rendering
    render () {
        const suggestionsHeader = (
            <div className="mdl-card__title mdl-card--border">
                <div className="mdl-card__title-text">
                    {_.isEmpty(this.state.error)
                        ? `Add suggested mapping rules`
                        :`${_.size(this.state.error)} errors saving suggestions`
                    }
                </div>
                <ContextMenu
                    className="ecc-silk-mapping__ruleslistmenu"
                >
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-select-all"
                        onClick={this.checkAll}
                    >
                        Select all
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-select-none"
                        onClick={this.checkNone}
                    >
                        Select none
                    </MenuItem>
                </ContextMenu>
            </div>
        );

        const suggestionsList = !_.isEmpty(this.state.error) ? false :
            _.map(this.state.data, (value, key) => {
            return _.map(value, (item, i) => <SuggestionView
                item={item}
                i={i}
                k={key}
                check={this.check}
                checked={this.isChecked(key, i)}
            />
        )});

        const errorsList = _.isEmpty(this.state.error) ? false :
            _.map(this.state.error, (err) => <li className="ecc-silk-mapping__ruleitem mdl-list__item ecc-silk-mapping__ruleitem--literal ecc-silk-mapping__ruleitem--summary ">
                <div className="mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content">
                    <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                        {err.rule.mappingTarget.uri}
                    </div>
                    <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                        {err.rule.sourcePath}
                    </div>
                    <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                        <Error>{err.msg.message}</Error>
                    </div>
                </div>
            </li>
        );

        const actions = <div className="mdl-card__actions mdl-card__actions--fixed mdl-card--border">
            {_.isEmpty(this.state.error)?<AffirmativeButton onClick={this.handleAddSuggestions} >Save</AffirmativeButton>:false}
            <DismissiveButton onClick={this.props.onClose} >Cancel</DismissiveButton>
        </div>

        if (this.state.loading) {
            return <Spinner/>;
        }
        else {
            return <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    {suggestionsHeader}
                    <ol className="mdl-list">
                        {suggestionsList}
                        {errorsList}
                    </ol>
                    {actions}
                </div>
            </div>
        }
    }
});

export default SuggestionsView;
