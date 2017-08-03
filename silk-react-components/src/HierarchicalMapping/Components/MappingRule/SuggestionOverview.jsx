import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Spinner,
    Error,
    Checkbox,
    Info,
    Button,
    AffirmativeButton,
    DismissiveButton,
    ContextMenu,
    MenuItem,
    Chip,
} from 'ecc-gui-elements';
import SuggestionView from './SuggestionView';
import hierarchicalMappingChannel from '../../store';
import {ParentElement} from './SharedComponents';
import _ from 'lodash';

let pendingRules = {};
let wrongRules = {};
const SuggestionOverview = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        targetClassUris: React.PropTypes.array,
    },
    check(value, id, event) {
        const item = {
            suggestedClass: value,
            pos: id,
        };
        this.setState({
            checked: _.some(this.state.checked, item)
                ? _.filter(this.state.checked, v => !_.isEqual(v, item))
                : _.concat(this.state.checked, [item]),
        });
    },
    isChecked(key, i) {
        const item = {
            suggestedClass: key,
            pos: i,
        };
        return _.some(this.state.checked, item);
    },
    loadData() {
        this.setState({
            loading: true,
        });
        pendingRules = {};
        wrongRules = {};
        hierarchicalMappingChannel
            .request({
                topic: 'rule.suggestions',
                data: {
                    targetClassUris: this.props.targetClassUris,
                    ruleId: this.props.ruleId,
                },
            })
            .subscribe(
                response => {
                    this.setState({
                        loading: false,
                        data: response.suggestions,
                    });
                },
                err => {
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
        const correspondences = [];
        this.setState({
            loading: true,
        });
        _.map(this.state.checked, ({suggestedClass, pos}) => {
            correspondences.push({
                sourcePath: this.state.data[suggestedClass][pos].uri,
                targetProperty: suggestedClass,
            });
        });
        hierarchicalMappingChannel
            .request({
                topic: 'rules.generate',
                data: {
                    correspondences,
                    parentRuleId: this.props.ruleId,
                },
            })
            .subscribe(
                response => {
                    this.setState({loading: true});
                    _.map(response.rules, (rule, k) => {
                        this.saveRule(
                            {...rule, parentId: this.props.ruleId},
                            k,
                        );
                    });
                    hierarchicalMappingChannel.subject('reload').onNext(true);
                },
                err => {
                    console.warn('err MappingRuleOverview: rule.suggestions');
                    this.setState({loading: false});
                },
            );
    },
    saveRule(rule, pos) {
        rule.id = undefined;
        pendingRules[pos] = true;
        hierarchicalMappingChannel
            .request({
                topic: 'rule.createGeneratedMapping',
                data: {...rule},
            })
            .subscribe(
                () => {
                    this.onSafeDone(pos);
                },
                err => {
                    wrongRules[pos] = {
                        msg: err,
                        rule,
                    };
                    this.onSafeDone(pos);
                },
                () => {},
            );
    },
    onSafeDone(pos) {
        delete pendingRules[pos];
        if (_.size(pendingRules) === 0) {
            if (_.size(wrongRules) > 0) {
                this.setState({
                    loading: false,
                    error: wrongRules,
                });
            } else {
                hierarchicalMappingChannel
                    .subject('ruleView.close')
                    .onNext({id: 0});
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
        const checked = [];
        event.stopPropagation();
        _.map(this.state.data, (value, key) => {
            _.map(value, (e, i) => {
                checked.push({suggestedClass: key, pos: i});
            });
        });
        this.setState({checked});
    },
    checkNone(event) {
        event.stopPropagation();
        this.setState({
            checked: false,
        });
    },
    // template rendering
    render() {
        const suggestionsMenu = !_.isEmpty(this.state.error)
            ? false
            : <ContextMenu className="ecc-silk-mapping__ruleslistmenu">
                  <MenuItem
                      className="ecc-silk-mapping__ruleslistmenu__item-select-all"
                      onClick={this.checkAll}>
                      Select all
                  </MenuItem>
                  <MenuItem
                      className="ecc-silk-mapping__ruleslistmenu__item-select-none"
                      onClick={this.checkNone}>
                      Select none
                  </MenuItem>
              </ContextMenu>;

        const suggestionsHeader = (
            <div className="mdl-card__title mdl-card--border">
                <div className="mdl-card__title-text">
                    {_.isEmpty(this.state.error)
                        ? `Add suggested mapping rules`
                        : `${_.size(
                              this.state.error,
                          )} errors saving suggestions`}
                </div>
                {suggestionsMenu}
            </div>
        );

        const suggestionsList = !_.isEmpty(this.state.error)
            ? false
            : _.map(this.state.data, (value, suggestedClass) =>
                  _.map(value, (item, pos) =>
                      <SuggestionView
                          item={item}
                          pos={pos}
                          suggestedClass={suggestedClass}
                          check={this.check}
                          checked={this.isChecked(suggestedClass, pos)}
                      />,
                  ),
              );

        const errorsList = _.isEmpty(this.state.error)
            ? false
            : _.map(this.state.error, err =>
                  <li className="ecc-silk-mapping__ruleitem mdl-list__item ecc-silk-mapping__ruleitem--literal ecc-silk-mapping__ruleitem--summary ">
                      <div className="mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content">
                          <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                              {err.rule.mappingTarget.uri}
                          </div>
                          <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                              {err.rule.sourcePath}
                          </div>
                          <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                              <Error>
                                  {err.msg.message}
                              </Error>
                          </div>
                      </div>
                  </li>,
              );

        const actions = (
            <div className="mdl-card__actions mdl-card__actions--fixed mdl-card--border">
                {_.isEmpty(this.state.error)
                    ? <AffirmativeButton
                          className="ecc-hm-suggestions-save"
                          onClick={this.handleAddSuggestions}
                          disabled={_.size(this.state.checked) === 0}>
                          Save
                      </AffirmativeButton>
                    : false}

                <DismissiveButton 
                  onClick={this.props.onClose}
                  className="ecc-hm-suggestions-cancel">
                    Cancel
                </DismissiveButton>
            </div>
        );

        const suggestionsEmptyInfo =
            _.size(this.state.data) === 0
                ? <div className="mdl-card__content">
                      <Info vertSpacing border>
                          No suggestions found for{' '}
                          <ParentElement parent={this.props.parent} />.
                      </Info>
                  </div>
                : false;

        if (this.state.loading) {
            return <Spinner />;
        }
        return (
            <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
                <div className="mdl-card mdl-card--stretch">
                    {suggestionsHeader}
                    <ol className="mdl-list">
                        {suggestionsList}
                        {suggestionsEmptyInfo}
                        {errorsList}
                    </ol>
                    {actions}
                </div>
            </div>
        );
    },
});

export default SuggestionOverview;
