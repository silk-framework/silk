import React from 'react';
import {
    AffirmativeButton,
    Card,
    CardActions,
    CardContent,
    CardMenu,
    CardTitle,
    Checkbox,
    ConfirmationDialog,
    ContextMenu,
    DismissiveButton,
    DisruptiveButton,
    Error,
    Info,
    MenuItem,
    ProgressButton,
    ScrollingHOC,
    Spinner,
    Tooltip,
    Warning,
} from '@eccenca/gui-elements';
import _ from 'lodash';
import ErrorView from '../../components/ErrorView';
import SuggestionsRule from './SuggestionsRule';
import { generateRuleAsync, getSuggestionsAsync } from '../../store';
import { ParentElement } from '../../components/ParentElement';
import { SUGGESTION_TYPES } from '../../utils/constants';
import PropTypes from 'prop-types';

const SuggestionsListWrapper = props => (
    <div className="ecc-silk-mapping__ruleslist ecc-silk-mapping__suggestionlist">
        <Card fixedActions>{props.children}</Card>
    </div>
);

class SuggestionsList extends React.Component {
    static propTypes = {
        targetClassUris: PropTypes.array,
        onAskDiscardChanges: PropTypes.func,
    };

    state = {
        data: undefined,
        error: false,
        showDefaultProperties: true,
        rawData: undefined,
        checked: false,
        matchFromDataset: true,
        warnings: [],
    };
    count = 0;
    
    constructor(props) {
        super(props);
        this.onChecked = this.onChecked.bind(this);
        this.onTypeChanged = this.onTypeChanged.bind(this);
        this.handleAddSuggestions = this.handleAddSuggestions.bind(this);
        this.toggleDefaultProperties = this.toggleDefaultProperties.bind(this);
        this.checkAll = this.checkAll.bind(this);
        this.checkNone = this.checkNone.bind(this);
    }
    

    onChecked(v) {
        const data = this.state.data;
        const index = _.findIndex(data, d => d.id === v.id);
        data[index].checked = !data[index].checked;
        this.setState({ data });
    }

    loadData() {
        this.setState({
            loading: true,
        });
        getSuggestionsAsync({
            targetClassUris: this.props.targetClassUris,
            ruleId: this.props.ruleId,
            matchFromDataset: this.state.matchFromDataset,
        }).subscribe(
            response => {
                const rawData = _.map(response.suggestions, value => ({
                    ...value,
                    checked: false,
                    type: value.type || SUGGESTION_TYPES[0],
                }));
                this.setState({
                    warnings: response.warnings.filter(value => !_.isEmpty(value)),
                    loading: false,
                    rawData,
                    data: this.state.showDefaultProperties
                        ? rawData
                        : rawData.filter(value => !!value.targetProperty),
                });
            },
            err => {
                this.setState({ loading: false, error: [{ error: err }] });
            }
        );
    }

    onTypeChanged(v) {
        
        const data = this.state.data;
        const index = _.findIndex(data, d => d.id === v.id);
        data[index].type = v.type;
        this.setState({ data });
    }

    componentDidMount() {
        this.loadData();
    }

    componentDidUpdate() {
        if (_.get(this, 'state.data', false) && this.count++ === 0) {
            // Scroll should only happen once!
            this.props.scrollIntoView({
                topOffset: 75,
            });
        }
    }

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

        generateRuleAsync(correspondences, this.props.ruleId).subscribe(
            () => {
                this.props.onClose();
            },
            err => {
                // If we have a list of failedRules, we want to show them, otherwise something
                // else failed
                const error = err.failedRules
                    ? err.failedRules
                    : [{ error: err }];
                this.setState({ saving: false, error });
            }
        );
    }

    toggleDefaultProperties() {
        if (this.state.data.filter(value => value.checked).length !== 0) {
            this.props.onAskDiscardChanges(true);
        } else {
            this.setState({
                data: !this.state.showDefaultProperties
                    ? this.state.rawData
                    : this.state.rawData.filter(value => !!value.targetProperty),
                showDefaultProperties: !this.state.showDefaultProperties,
            });
        }
    }

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
    }

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
    }
    
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
                            tooltip="Progress"
                        >
                            Save
                        </ProgressButton>
                        <DismissiveButton
                            raised
                            disabled
                            className="ecc-hm-suggestions-cancel"
                        >
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
                            className="ecc-hm-suggestions-cancel"
                        >
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </SuggestionsListWrapper>
            );
        }

        let suggestionsList = false;
        const hasChecks = _.get(this.state, 'checked');
        const errors = _.filter(this.state.warnings, warning => warning.code !== 404);
        const warnings = _.filter(this.state.warnings, warning => warning.code === 404);

        const errorsComponent = !_.isEmpty(errors) && (
            <Error
                className="ecc-hm-suggestions__errors-container"
            >
                {_.map(
                    errors,
                    error => (
                        <div
                            key={error.code + error.detail + error.title}
                            className="ecc-hm-suggestions-error"
                        >
                            <b>{error.title} ({error.code})</b>
                            <div>{error.detail}</div>
                        </div>
                    ),
                )}
            </Error>
        );

        const warningsComponent = !_.isEmpty(warnings) && (
            <Warning
                className="ecc-hm-suggestions__warnings-container"
            >
                {_.map(
                    warnings,
                    warning => (
                        <div
                            key={warning.code + warning.detail + warning.title}
                            className="ecc-hm-suggestions-warning"
                        >
                            <b>{warning.title} ({warning.code})</b>
                            <div>{warning.detail}</div>
                        </div>
                    ),
                )}
            </Warning>
        );
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
                                    <div
                                        className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__suggestitem-headline"
                                    >
                                        Value path
                                    </div>
                                    <div
                                        className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__suggestitem-subline"
                                    >
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
                                onClick={this.toggleDefaultProperties}
                            >
                                {this.state.showDefaultProperties
                                    ? 'Hide'
                                    : 'Show'}{' '}
                                default properties
                            </MenuItem>
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
                    </CardMenu>
                </CardTitle>
                {warningsComponent}
                {errorsComponent}
                {suggestionsList}
                <CardActions fixed>
                    <AffirmativeButton
                        raised
                        id="suggestion-save-btn"
                        className="ecc-hm-suggestions-save"
                        onClick={this.handleAddSuggestions}
                        disabled={_.size(suggestionsToBeSave) === 0}
                    >
                        Save
                    </AffirmativeButton>
                    <DismissiveButton
                        raised
                        onClick={this.props.onClose}
                        className="ecc-hm-suggestions-cancel"
                    >
                        Cancel
                    </DismissiveButton>
                </CardActions>
            </SuggestionsListWrapper>
        );
    }
}

export default ScrollingHOC(SuggestionsList);
