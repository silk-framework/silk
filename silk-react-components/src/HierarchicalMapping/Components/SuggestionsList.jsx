import React from 'react';
import {
    AffirmativeButton,
    DismissiveButton,
    Card,
    CardTitle,
    CardMenu,
    CardContent,
    CardActions,
    Info,
    ContextMenu,
    MenuItem,
    Spinner,
    ScrollingMixin,
    Checkbox,
    ProgressButton,
} from 'ecc-gui-elements';
import _ from 'lodash';
import ErrorView from './MappingRule/ErrorView';
import UseMessageBus from '../UseMessageBusMixin';
import SuggestionsRule from './SuggestionsRule';
import hierarchicalMappingChannel from '../store';
import {ParentElement} from './MappingRule/SharedComponents';

const SuggestionsListWrapper = props => (
    <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
        <Card fixedActions>{props.children}</Card>
    </div>
);

const SuggestionsList = React.createClass({
    mixins: [UseMessageBus, ScrollingMixin],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        targetClassUris: React.PropTypes.array,
    },
    check(suggestion) {
        this.setState({
            checked: _.includes(this.state.checked, suggestion.id)
                ? _.without(this.state.checked, suggestion.id)
                : _.concat(this.state.checked, suggestion.id),
        });
    },
    isChecked(suggestion) {
        return _.includes(this.state.checked, suggestion.id);
    },
    loadData() {
        this.setState({
            loading: true,
        });
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
                    this.setState({loading: false, error: [{error: err}]});
                }
            );
    },
    componentDidMount() {
        this.loadData();
    },
    componentDidUpdate() {
        if (_.get(this, 'state.data', false)) {
            this.scrollIntoView({
                topOffset: 75,
            });
        }
    },
    handleAddSuggestions(event) {
        event.stopPropagation();
        const correspondences = [];
        this.setState({
            saving: true,
        });

        _.forEach(this.state.data, suggestion => {
            if (this.isChecked(suggestion)) {
                correspondences.push({
                    sourcePath: suggestion.sourcePath,
                    targetProperty: suggestion.targetProperty,
                });
            }
        });

        hierarchicalMappingChannel
            .request({
                topic: 'rules.generate',
                data: {
                    correspondences,
                    parentId: this.props.ruleId,
                },
            })
            .subscribe(
                () => {
                    hierarchicalMappingChannel
                        .subject('ruleView.close')
                        .onNext({id: 0});
                    this.props.onClose();
                    hierarchicalMappingChannel.subject('reload').onNext(true);
                },
                err => {
                    // If we have a list of failedRules, we want to show them, otherwise something
                    // else failed
                    const error = err.failedRules
                        ? err.failedRules
                        : [{error: err}];

                    this.setState({saving: false, error});

                    hierarchicalMappingChannel.subject('reload').onNext(true);
                }
            );
    },
    getInitialState() {
        return {
            data: undefined,
            checked: [],
            error: false,
        };
    },
    checkAll(event) {
        if (event.stopPropagation) {
            event.stopPropagation();
        }
        this.setState({
            checked: _.map(this.state.data, suggestion => suggestion.id),
        });
    },
    checkNone(event) {
        if (event.stopPropagation) {
            event.stopPropagation();
        }
        this.setState({
            checked: [],
        });
    },
    // template rendering
    render() {
        if (this.state.loading) {
            return <Spinner />;
        }

        if (this.state.saving) {
            return (
                <SuggestionsListWrapper>
                    <CardTitle>Saving...</CardTitle>
                    <CardContent>
                        <p>
                            The {_.size(this.state.checked)} rules you have
                            selected are being created.
                        </p>
                    </CardContent>
                    <CardActions fixed>
                        <ProgressButton
                            progress={0}
                            progressTopic={hierarchicalMappingChannel.subject(
                                'rule.suggestions.progress'
                            )}
                            tooltip={'Progress'}>
                            Save
                        </ProgressButton>
                        <DismissiveButton
                            raised
                            disabled
                            className="ecc-hm-suggestions-cancel">
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </SuggestionsListWrapper>
            );
        }

        if (this.state.error) {
            const errorsList = _.map(this.state.error, err => (
                <li className="ecc-silk-mapping__ruleitem mdl-list__item ecc-silk-mapping__ruleitem--literal ecc-silk-mapping__ruleitem--summary ">
                    <div className="mdl-list__item-primary-content ecc-silk-mapping__ruleitem-content">
                        <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                            {_.get(err, 'rule.sourcePath', '')}
                        </div>
                        <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                            {_.get(err, 'rule.mappingTarget.uri', '')}
                        </div>
                        <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-subline">
                            <ErrorView {...err.error} />
                        </div>
                    </div>
                </li>
            ));

            return (
                <SuggestionsListWrapper>
                    <CardTitle>Saving suggestions returned errors</CardTitle>
                    <ol className="mdl-list">{errorsList}</ol>
                    <CardActions>
                        <DismissiveButton
                            raised
                            onClick={this.props.onClose}
                            className="ecc-hm-suggestions-cancel">
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </SuggestionsListWrapper>
            );
        }

        let suggestionsList = false;
        const hasChecks = _.some(this.state.checked);

        if (_.size(this.state.data) === 0) {
            suggestionsList = (
                <CardContent>
                    <Info vertSpacing border>
                        No suggestions found for{' '}
                        <ParentElement parent={this.props.parent} />.
                    </Info>
                </CardContent>
            );
        } else {
            const suggestions = _.orderBy(
                this.state.data,
                ['sourcePath', 'order'],
                ['asc', 'desc']
            );

            suggestionsList = (
                <ol className="mdl-list">
                    <li className="ecc-silk-mapping__ruleitem ecc-silk-mapping__ruleitem--literal">
                        <div className="ecc-silk-mapping__ruleitem-summary">
                            <div className="mdl-list__item">
                                <Checkbox
                                    onChange={
                                        hasChecks
                                            ? this.checkNone
                                            : this.checkAll
                                    }
                                    checked={hasChecks}
                                    className="ecc-silk-mapping__suggestitem-checkbox"
                                    ripple
                                />
                                <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline">
                                    Value path
                                </div>
                                <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">
                                    Target property
                                </div>
                            </div>
                        </div>
                    </li>
                    {_.map(suggestions, suggestion => (
                        <SuggestionsRule
                            suggestion={suggestion}
                            check={this.check}
                            checked={this.isChecked(suggestion)}
                            key={suggestion.id}
                        />
                    ))}
                </ol>
            );
        }

        return (
            <SuggestionsListWrapper>
                <CardTitle>
                    <div className="mdl-card__title-text">
                        Add suggested mapping rules
                    </div>
                    <CardMenu>
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
                    </CardMenu>
                </CardTitle>
                {suggestionsList}
                <CardActions fixed>
                    <AffirmativeButton
                        raised
                        className="ecc-hm-suggestions-save"
                        onClick={this.handleAddSuggestions}
                        disabled={_.size(this.state.checked) === 0}>
                        Save
                    </AffirmativeButton>
                    <DismissiveButton
                        raised
                        onClick={this.props.onClose}
                        className="ecc-hm-suggestions-cancel">
                        Cancel
                    </DismissiveButton>
                </CardActions>
            </SuggestionsListWrapper>
        );
    },
});

export default SuggestionsList;
