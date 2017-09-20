import React from 'react';
import {
    AffirmativeButton,
    DismissiveButton,
    Card,
    CardTitle,
    CardMenu,
    CardContent,
    CardActions,
    Error,
    Info,
    ContextMenu,
    MenuItem,
    Spinner,
    ScrollingMixin,
} from 'ecc-gui-elements';
import _ from 'lodash';
import UseMessageBus from '../UseMessageBusMixin';
import SuggestionsRule from './SuggestionsRule';
import hierarchicalMappingChannel from '../store';
import {ParentElement} from './MappingRule/SharedComponents';

let pendingRules = {};
let wrongRules = {};
const SuggestionsList = React.createClass({
    mixins: [UseMessageBus, ScrollingMixin],

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
                    this.setState({loading: false});
                }
            );
    },
    componentDidMount() {
        this.loadData();
    },
    componentDidUpdate() {
        if (_.get(this, 'state.data', false)) {
            this.scrollIntoView({
                topOffset: 75
            });
        }
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
                    // TODO: DROP root
                    parentRuleId: _.get(this.props, 'ruleId', 'root'),
                },
            })
            .subscribe(
                response => {
                    this.setState({loading: true});
                    _.map(response.rules, (rule, k) => {
                        this.saveRule(
                            {...rule, parentId: this.props.ruleId},
                            k
                        );
                    });
                    hierarchicalMappingChannel.subject('reload').onNext(true);
                },
                err => {
                    this.setState({loading: false});
                }
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
                () => {}
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
            : <CardMenu>
                  <ContextMenu className="ecc-silk-mapping__ruleslistmenu">
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
                  </ContextMenu>
              </CardMenu>;

        const suggestionsHeader = (
            <CardTitle>
                <div className="mdl-card__title-text">
                    {_.isEmpty(this.state.error)
                        ? `Add suggested mapping rules`
                        : `${_.size(
                              this.state.error
                          )} errors saving suggestions`}
                </div>
                {suggestionsMenu}
            </CardTitle>
        );

        const suggestionsList = !_.isEmpty(this.state.error)
            ? false
            : _.map(this.state.data, (value, suggestedClass) =>
                  _.map(value, (item, pos) =>
                      <SuggestionsRule
                          item={item}
                          pos={pos}
                          suggestedClass={suggestedClass}
                          check={this.check}
                          checked={this.isChecked(suggestedClass, pos)}
                      />
                  )
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
                  </li>
              );

        const actions = (
            <CardActions fixed>
                {_.isEmpty(this.state.error)
                    ? <AffirmativeButton
                          raised
                          className="ecc-hm-suggestions-save"
                          onClick={this.handleAddSuggestions}
                          disabled={_.size(this.state.checked) === 0}>
                          Save
                      </AffirmativeButton>
                    : false}

                <DismissiveButton
                    raised
                    onClick={this.props.onClose}
                    className="ecc-hm-suggestions-cancel">
                    Cancel
                </DismissiveButton>
            </CardActions>
        );

        const suggestionsEmptyInfo =
            _.size(this.state.data) === 0
                ? <CardContent>
                      <Info vertSpacing border>
                          No suggestions found for{' '}
                          <ParentElement parent={this.props.parent} />.
                      </Info>
                  </CardContent>
                : false;

        if (this.state.loading) {
            return <Spinner />;
        }
        return (
            <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
                <Card fixedActions>
                    {suggestionsHeader}
                    <ol className="mdl-list">
                        {suggestionsList}
                        {suggestionsEmptyInfo}
                        {errorsList}
                    </ol>
                    {actions}
                </Card>
            </div>
        );
    },
});

export default SuggestionsList;
