import React from 'react';
import _ from 'lodash';
import {
    Card,
    CardTitle,
    CardMenu,
    CardContent,
    FloatingActionList,
    Info,
} from 'ecc-gui-elements';
import MappingRule from './MappingRule/MappingRule';
import Navigation from '../Mixins/Navigation';

const MappingsList = React.createClass({
    mixins: [Navigation],

    // define property types
    propTypes: {
        rules: React.PropTypes.array.isRequired,
    },

    getDefaultProps() {
        return {
            rules: [],
        };
    },

    // template rendering
    render() {

        const {
            rules
        } = this.props;

        const listTitle = (
            <CardTitle>
                <div className="mdl-card__title-text">
                    Mapping rules {`(${rules.length})`}
                </div>
            </CardTitle>
        );

        const listItems = _.isEmpty(rules)
            ? <CardContent>
                  <Info vertSpacing border>
                      No existing mapping rules.
                  </Info>
                  {/* TODO: we should provide options like adding rules or suggestions here,
                         even a help text would be a good support for the user.
                         */}
              </CardContent>
            : <ol className="mdl-list">
                  {_.map(rules, (rule, idx) =>
                      <MappingRule
                          pos={idx}
                          parentId={this.props.currentRuleId}
                          count={rules.length}
                          key={`MappingRule_${rule.id}`}
                          {...rule}
                      />
                  )}
              </ol>;

        const listActions = (
            <FloatingActionList
                fabSize="large"
                fixed
                iconName="add"
                actions={[
                    {
                        icon: 'insert_drive_file',
                        label: 'Add value mapping',
                        handler: () => {
                            this.handleCreate({
                                type: 'direct',
                            });
                        },
                    },
                    {
                        icon: 'folder',
                        label: 'Add object mapping',
                        handler: () => {
                            this.handleCreate({
                                type: 'object',
                            });
                        },
                    },
                    {
                        icon: 'lightbulb_outline',
                        label: 'Suggest mappings',
                        handler: this.handleShowSuggestions,
                    },
                ]}
            />
        );

        return (
            <div className="ecc-silk-mapping__ruleslist">
                <Card shadow={0}>
                    {listTitle}
                    {listItems}
                    {listActions}
                </Card>
            </div>
        );
    }

});

export default MappingsList;
