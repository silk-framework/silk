import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Spinner,
    Error,
    Checkbox,
    Button,
    ContextMenu,
    MenuItem,
    Chip,
} from 'ecc-gui-elements';
import SuggestionView from './SuggestionView';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';


const SuggestionsView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        targets: React.PropTypes.array,

    },
    expand(k, i, event) {
        this.setState({
            expanded: _.includes(this.state.expanded, k)
                ? _.filter(this.state.expanded, (e) => k !== e)
                : _.concat(this.state.expanded, [k])
        });
    },
    changed(value, id, event) {
        const i = `${value}-${id}`;
        this.setState({
            checked: _.includes(this.state.checked, i)
                ? _.filter(this.state.checked, (v) => v !== i)
                : _.concat(this.state.checked, [i])
        })
    },
    isExpanded(key,i){

        return _.includes(this.state.expanded, key)
    },
    isChecked(key,i){
        return _.includes(this.state.checked,`${key}-${i}`)
    },
    componentDidMount() {
            this.setState({
                loading: true,
            });
            hierarchicalMappingChannel.request({
                topic: 'transform.get',
            }).subscribe(
                (transform) => {
                    hierarchicalMappingChannel.request(
                        {
                            topic: 'rule.suggestions',
                            data: {targets: this.props.targets, dataset: transform.example.selection.inputId}
                        }).subscribe(
                        (response) => {
                            this.setState({
                                loading: false,
                                data: response,
                            });
                        },
                        (err) => {
                            console.warn('err MappingRuleOverview: rule.suggestions');
                            this.setState({loading: false});
                        },
                        () => {this.setState({loading: false})}
                    );
                },
                (err) => {
                    console.warn('err MappingRuleOverview: transform.get');
                    this.setState({loading: false});
                }
            )
    },
    getInitialState() {
        return {
            data: undefined,
            expanded: [],
            checked: [],
        };
    },

    // template rendering
    render () {

        const suggestionsHeader = (
            <div className="mdl-card__title mdl-card--border">
                <div className="mdl-card__title-text">
                    Add suggested mapping rules ({_.sum(_.map(this.state.data, v => v.length))})
                </div>
                <ContextMenu
                    className="ecc-silk-mapping__ruleslistmenu"
                >
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-add-value"

                    >
                        Select all
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-add-object"

                    >
                        Select none
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-autosuggest"

                    >
                        Select entity prop.
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-expand"

                    >
                        Select source matches
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-reduce"

                    >
                        Hide unselected
                    </MenuItem>
                </ContextMenu>
            </div>

        );

        const suggestionsList = _.map(this.state.data, (value, key) => {
            return _.map(_.filter(value,(e,x)=> !(x===0&&this.isExpanded(e, x))), (item, i) => <SuggestionView
                item={item}
                i={i}
                k={key}
                check={this.changed}
                expand={this.expand}
                expanded={this.isExpanded(key, i)}
                checked={this.isChecked(key, i)}
            />
        )});

        const actions = <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
            <div className="mdl-card__actions mdl-card--border">
                <Button accent onClick={this.props.onClose} >Save</Button>
                <Button onClick={this.props.onClose} >Close</Button>
            </div>
        </div>

        if (_.isUndefined(this.state.data)) {
            return <Spinner/>;
        }
        else {
            return <div className="ecc-silk-mapping__ruleslist mdl-card mdl-card--stretch mdl-shadow--2dp">
                {suggestionsHeader}
                {actions}
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    <ol className="mdl-list">
                        {suggestionsList}
                    </ol>
                </div>
                {actions}
            </div>

        }
    }
});

export default SuggestionsView;
