import React from 'react';
import {
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
    Card,
    CardTitle,
    CardMenu,
    CardContent,
    CardActions,
    ConfirmationDialog,
    Info,
    Error,
    ContextMenu,
    MenuItem,
    Spinner,
    ScrollingMixin,
    Checkbox,
    ProgressButton,
    Tooltip,
} from '@eccenca/gui-elements';
import _ from 'lodash';
import ErrorView from './MappingRule/ErrorView';
import UseMessageBus from '../UseMessageBusMixin';
import SuggestionsRule from './SuggestionsRule';
import hierarchicalMappingChannel from '../store';
import {ParentElement} from './MappingRule/SharedComponents';
import {SUGGESTION_TYPES} from '../helpers';

const SuggestionsListWrapper = props => (
    <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
        <Card fixedActions>{props.children}</Card>
    </div>
);

const SuggestionsList = React.createClass({
    mixins: [UseMessageBus, ScrollingMixin],
    defaultCheckValue: false,
    // define property types
    // FIXME: check propTypes
    propTypes: {
        targetClassUris: React.PropTypes.array,
    },
    getInitialState() {
        return {
            data: undefined,
            error: false,
            showDefaultProperties: true,
            rawData: undefined,
            askForDiscard: false,
            checked: this.defaultCheckValue,
            matchFromDataset: true
        };
    },
    onChecked(v) {
        const data = this.state.data;
        const index = _.findIndex(data, d => d.id === v.id);
        data[index].checked = !data[index].checked;
        this.setState({data});
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
                    matchFromDataset: this.state.matchFromDataset,
                },
            })
            .subscribe(
                response => {
                    const rawData = _.map(response.suggestions, v => ({
                        ...v,
                        checked: this.defaultCheckValue,
                        type: v.type || SUGGESTION_TYPES[0],
                    }));
                    this.setState({
                        warnings: response.warnings,
                        loading: false,
                        rawData,
                        data: this.state.showDefaultProperties
                            ? rawData
                            : rawData.filter(v => !!v.targetProperty),
                    });
                },
                err => {
                    this.setState({loading: false, error: [{error: err}]});
                }
            );
    },
    onTypeChanged(v) {
        const data = this.state.data;
        const index = _.findIndex(data, d => d.id === v.id);
        data[index].type = v.type;
        this.setState({data});
    },
    componentDidMount() {
        this.loadData();
    },
    count: 0,
    componentDidUpdate() {
        if (_.get(this, 'state.data', false) && this.count++ === 0) {
            // Scroll should only happen once!
            this.scrollIntoView({
                topOffset: 75,
            });
        }
    },
    discardDialog() {
        return (
            <ConfirmationDialog
                active
                modal
                title="Discard selection?"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.onDiscard}>
                        Discard
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.onCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>You currently selection will be lost.</p>
            </ConfirmationDialog>
        );
    },
    handleAddSuggestions(event) {
        event.stopPropagation();

        this.setState({
            saving: true,
        });

        const correspondences = this.state.data
            .filter(v => v.checked)
            .map(v => ({
                sourcePath: v.sourcePath,
                targetProperty: v.targetProperty,
                type: v.type,
            }));
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
                    hierarchicalMappingChannel.subject('reload').onNext(true);
                    this.props.onClose();
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
    toggleDefaultProperties() {
        if (this.state.data.filter(v => v.checked).length !== 0) {
            this.setState({askForDiscard: true});
        } else {
            this.setState({
                data: !this.state.showDefaultProperties
                    ? this.state.rawData
                    : this.state.rawData.filter(v => !!v.targetProperty),
                showDefaultProperties: !this.state.showDefaultProperties,
            });
        }
    },
    checkAll(event) {
        if (event.stopPropagation) {
            event.stopPropagation();
        }
        this.setState({
            data: this.state.data.map(a => ({
                ...a,
                checked: true,
            })),
            checked: true,
        });
    },
    checkNone(event) {
        if (event.stopPropagation) {
            event.stopPropagation();
        }
        this.setState({
            data: this.state.data.map(a => ({
                ...a,
                checked: false,
            })),
            checked: false,
        });
    },
    onDiscard() {
        this.setState({
            data: !this.state.showDefaultProperties
                ? this.state.rawData
                : this.state.rawData.filter(v => !!v.targetProperty),
            showDefaultProperties: !this.state.showDefaultProperties,
            askForDiscard: false,
        });
    },
    onCancelDiscard() {
        this.setState({askForDiscard: false});
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
                            The{' '}
                            {_.size(_.filter(this.state.data, d => d.checked))}{' '}
                            rules you have selected are being created.
                        </p>
                    </CardContent>
                    <CardActions fixed>
                        <ProgressButton
                            progress={0}
                            id="suggestion-save-btn"
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
        const hasChecks = _.get(this.state, 'checked');
        const warnings = _.isEmpty(this.state.warnings) && (
            <Error>
                {_.map(
                    this.state.warnings,
                    warn => (warn.title && warn.detail) ?
                        <div><b>{warn.title}</b><div>{warn.detail}</div></div>
                )}
            </Error>
        );

        if (_.size(this.state.data) === 0) {
            suggestionsList = (
                <CardContent>
                    <Info vertSpacing border>
                        No suggestions found for{' '}
                        <ParentElement parent={this.props.parent} />.
                    </Info>
                    {warnings}
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
                    <li className="ecc-silk-mapping__ruleitem">
                        <div className="ecc-silk-mapping__ruleitem-summary">
                            <div className="mdl-list__item ecc-silk-mapping__ruleheader">
                                <div className="ecc-silk-mapping__suggestitem-checkbox">
                                    <Tooltip label="Select all">
                                        <div>
                                            <Checkbox
                                                onChange={
                                                    hasChecks
                                                        ? this.checkNone
                                                        : this.checkAll
                                                }
                                                checked={hasChecks}
                                            />
                                        </div>
                                    </Tooltip>
                                </div>
                                <div className="mdl-list__item-primary-content">
                                    <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline">
                                        Value path
                                    </div>
                                    <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline">
                                        Target property
                                    </div>
                                    <div className="ecc-silk-mapping__suggestitem-typeselect">
                                        Mapping type
                                    </div>
                                </div>
                            </div>
                        </div>
                    </li>
                    {_.map(suggestions, suggestion => (
                        <SuggestionsRule
                            suggestion={suggestion}
                            onChecked={this.onChecked}
                            onTypeChanged={this.onTypeChanged}
                            pos={suggestion.id}
                            key={
                                suggestion.id +
                                suggestion.checked +
                                suggestion.type
                            }
                        />
                    ))}
                </ol>
            );
        }

        const suggestionsToBeSave = _.filter(
            this.state.data,
            entry => entry.checked
        );
        const confirmDialog = this.state.askForDiscard
            ? this.discardDialog()
            : false;
        return (
            <SuggestionsListWrapper>
                {confirmDialog}
                <CardTitle>
                    <div className="mdl-card__title-text">
                        Add suggested mapping rules
                    </div>
                    <CardMenu>
                        <ContextMenu className="ecc-silk-mapping__ruleslistmenu">
                            <MenuItem
                                className="ecc-silk-mapping__ruleslistmenu__item-select-all"
                                onClick={this.toggleDefaultProperties}>
                                {this.state.showDefaultProperties
                                    ? 'Hide'
                                    : 'Show'}{' '}
                                default properties
                            </MenuItem>
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
                {warnings}
                {suggestionsList}
                <CardActions fixed>
                    <AffirmativeButton
                        raised
                        id="suggestion-save-btn"
                        className="ecc-hm-suggestions-save"
                        onClick={this.handleAddSuggestions}
                        disabled={_.size(suggestionsToBeSave) === 0}>
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
