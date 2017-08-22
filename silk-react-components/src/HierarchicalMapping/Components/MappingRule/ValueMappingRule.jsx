import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Button,
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
    Card,
    CardTitle,
    CardContent,
    CardActions,
    Info,
} from 'ecc-gui-elements';
import ExampleView from './ExampleView';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';
import ValueMappingRuleForm from './Forms/ValueMappingRuleForm';
import {
    SourcePath,
    ThingName,
    ThingDescription,
    InfoBox,
    PropertyTypeLabel,
    PropertyTypeDescription,
} from './SharedComponents';

const RuleValueView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        // operator: React.PropTypes.object,
        type: React.PropTypes.string,
        sourcePath: React.PropTypes.string,
        mappingTarget: React.PropTypes.object,
        edit: React.PropTypes.bool.isRequired,
    },
    handleCloseEdit(obj) {
        if (obj.id === this.props.id) this.setState({edit: false});
    },
    componentDidMount() {
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.close'),
            this.handleCloseEdit,
        );
    },
    getInitialState() {
        this.subscribe(
            hierarchicalMappingChannel.request({
                topic: 'rule.getEditorHref',
                data: {id: this.props.id},
            }),
            ({href}) => this.setState({href}),
        );

        return {
            edit: this.props.edit,
            href: null,
        };
    },
    handleComplexEdit(event) {
        if (__DEBUG__) {
            event.stopPropagation();
            alert(
                'Normally this would open the complex editor (aka jsplumb view)',
            );
            return false;
        }
    },
    // open view in edit mode
    handleEdit(event) {
        event.stopPropagation();
        this.setState({
            edit: !this.state.edit,
        });
    },
    handleClose(event) {
        event.stopPropagation();
        hierarchicalMappingChannel
            .subject('ruleView.unchanged')
            .onNext({id: this.props.id});
    },
    getOperators(operator, accumulator) {
        if (_.has(operator, 'function')) {
            if (_.has(operator, 'inputs')) {
                _.forEach(
                    operator.inputs,
                    input =>
                        (accumulator = _.concat(
                            accumulator,
                            this.getOperators(input, []),
                        )),
                );
            }
            accumulator.push(operator.function);
        }

        return accumulator;
    },
    // template rendering
    render() {
        const {edit} = this.state;
        const paths = _.get(this, 'props.sourcePaths', []);
        const operators = this.getOperators(this.props.operator, []);

        if (edit) {
            return (
                <ValueMappingRuleForm
                    id={this.props.id}
                    parentId={this.props.parentId}
                />
            );
        }

        return (
            <div className="ecc-silk-mapping__rulesviewer">
                <Card shadow={0}>
                    <CardTitle
                        className="ecc-silk-mapping__rulesviewer__title clickable"
                        onClick={this.props.handleToggleExpand}>
                        <div className="mdl-card__title-text">
                            <ThingName
                                id={_.get(
                                    this.props,
                                    'mappingTarget.uri',
                                    false,
                                )}
                            />
                        </div>
                    </CardTitle>
                    <CardContent>
                        {_.get(this.props, 'mappingTarget.uri', false)
                            ? <div className="ecc-silk-mapping__rulesviewer__targetProperty">
                                  <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                      <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                          Target property
                                      </dt>
                                      <dd>
                                          <InfoBox>
                                              <div className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                                                  <ThingName
                                                      id={_.get(
                                                          this.props,
                                                          'mappingTarget.uri',
                                                          undefined,
                                                      )}
                                                  />
                                              </div>
                                              <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-main">
                                                  <code>
                                                      {_.get(
                                                          this.props,
                                                          'mappingTarget.uri',
                                                          undefined,
                                                      )}
                                                  </code>
                                              </div>
                                              <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                                                  <ThingDescription
                                                      id={_.get(
                                                          this.props,
                                                          'mappingTarget.uri',
                                                          undefined,
                                                      )}
                                                  />
                                              </div>
                                          </InfoBox>
                                      </dd>
                                  </dl>
                              </div>
                            : false}
                        {_.get(
                            this.props,
                            'mappingTarget.valueType.nodeType',
                            false,
                        )
                            ? <div className="ecc-silk-mapping__rulesviewer__propertyType">
                                  <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                      <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                          Data type
                                      </dt>
                                      <dd>
                                          <InfoBox>
                                              <div className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                                                  <PropertyTypeLabel
                                                      name={_.get(
                                                          this.props,
                                                          'mappingTarget.valueType.nodeType',
                                                          false,
                                                      )}
                                                  />
                                              </div>
                                              <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                                                  <PropertyTypeDescription
                                                      name={_.get(
                                                          this.props,
                                                          'mappingTarget.valueType.nodeType',
                                                          false,
                                                      )}
                                                  />
                                              </div>
                                          </InfoBox>
                                      </dd>
                                  </dl>
                              </div>
                            : false}
                        {this.props.type === 'direct' &&
                        _.get(this.props, 'sourcePath', false)
                            ? <div className="ecc-silk-mapping__rulesviewer__sourcePath">
                                  <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                      <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                          Value path
                                      </dt>
                                      <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                          <code>{this.props.sourcePath}</code>{' '}
                                          <Button
                                              iconName="edit"
                                              className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
                                              onClick={this.handleComplexEdit}
                                              href={this.state.href}
                                              tooltip="Convert value path to value formula"
                                          />
                                      </dd>
                                  </dl>
                              </div>
                            : false}
                        {this.props.type !== 'direct' &&
                        _.get(this.props, 'sourcePaths', false)
                            ? <div className="ecc-silk-mapping__rulesviewer__sourcePath">
                                  <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                      <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                          Value formula
                                      </dt>
                                      <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                          Formula uses {paths.length} value path{paths.length > 1 ? 's' : ''}:&nbsp;
                                          <code>{paths.join(', ')}</code>
                                          &nbsp;and {operators.length} operator
                                          function{operators.length > 1 ? 's' : ''}:&nbsp;
                                          <code>{operators.join(', ')}</code>.
                                          <Button
                                              iconName="edit"
                                              className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
                                              onClick={this.handleComplexEdit}
                                              href={this.state.href}
                                              tooltip="Edit value formula"
                                          />
                                      </dd>
                                  </dl>
                              </div>
                            : false}
                        {_.get(this.props, 'id', false)
                            ? <div className="ecc-silk-mapping__rulesviewer__examples">
                                  <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                      <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                          Examples of target data
                                      </dt>
                                      <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                          <ExampleView id={this.props.id} />
                                      </dd>
                                  </dl>
                              </div>
                            : false}
                        {_.get(this, 'props.metadata.description', false)
                            ? <div className="ecc-silk-mapping__rulesviewer__comment">
                                  <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                      <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                          Description
                                      </dt>
                                      <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                          {this.props.metadata.description}
                                      </dd>
                                  </dl>
                              </div>
                            : false}
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__ruleseditor__actionrow">
                        <Button
                            className="ecc-silk-mapping__ruleseditor__actionrow-edit"
                            onClick={this.handleEdit}>
                            Edit
                        </Button>
                        <DisruptiveButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-remove"
                            onClick={() =>
                                hierarchicalMappingChannel
                                    .subject('removeClick')
                                    .onNext({
                                        id: this.props.id,
                                        uri: this.props.mappingTarget.uri,
                                        type: this.props.type,
                                        parent: this.props.parentId,
                                    })}
                            disabled={false}>
                            Remove
                        </DisruptiveButton>
                    </CardActions>
                </Card>
            </div>
        );
    },
});

export default RuleValueView;
