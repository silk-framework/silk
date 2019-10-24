import React from 'react';
import PropTypes from 'prop-types';
import {
    CardActions,
    CardContent,
} from '@eccenca/gui-elements';
import _ from 'lodash';
import { getEditorHref, updateObjectMappingAsync } from '../../../store';
import ExampleView from '../ExampleView';
import ObjectMappingRuleForm from './ObjectMappingRuleForm';

import {
    isClonableRule,
    isCopiableRule,
    isDebugMode,
    isObjectRule,
    isRootRule,
    MESSAGES,
} from '../../../utils/constants';
import transformRuleOfObjectMapping from '../../../utils/transformRuleOfObjectMapping';
import EventEmitter from '../../../utils/EventEmitter';

import EditButton from '../Components/buttons/EditButton';
import CopyButton from '../Components/buttons/CopyButton';
import CloneButton from '../Components/buttons/CloneButton';
import DeleteButton from '../Components/buttons/DeleteButton';
import TargetProperty from '../Components/content/TargetProperty';
import ObjectEntityRelation from '../Components/content/ObjectEntityRelation';
import ObjectTypeRules from '../Components/content/ObjectTypeRules';
import ObjectSourcePath from '../Components/content/ObjectSourcePath';
import ObjectUriPattern from '../Components/content/ObjectUriPattern';
import ExampleTarget from '../Components/content/ExampleTarget';
import MetadataLabel from '../Components/content/MetadataLabel';
import MetadataDesc from '../Components/content/MetadataDesc';
import { SourcePath } from '../../../Components/SourcePath';

class ObjectMappingRule extends React.Component {
    static propTypes = {
        parentId: PropTypes.string.isRequired,
        parent: PropTypes.object,
        edit: PropTypes.oneOfType([
            PropTypes.bool,
            PropTypes.object
        ]).isRequired,
        ruleData: PropTypes.object.isRequired,
    };
    
    state = {
        edit: !!this.props.edit,
        href: '',
    };
    
    constructor(props) {
        super(props);
        this.editUriRule = this.editUriRule.bind(this);
        this.removeUriRule = this.removeUriRule.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        this.handleCloseEdit = this.handleCloseEdit.bind(this);
        this.handleCopy = this.handleCopy.bind(this);
        this.handleClone = this.handleClone.bind(this);
    }
    
    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
        if (_.has(this.props, 'ruleData.rules.uriRule.id')) {
            this.setState({
                href: getEditorHref(this.props.ruleData.rules.uriRule.id)
            });
        }
    }
    
    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
    }
    
    componentWillReceiveProps(nextProps) {
        if (_.has(nextProps, 'ruleData.rules.uriRule.id')) {
            this.setState({
                href: getEditorHref(_.get(nextProps, 'ruleData.rules.uriRule.id', ''))
            })
        }
    }
    
    editUriRule(event) {
        if (isDebugMode()) {
            event.stopPropagation();
            alert('Normally this would open the complex editor (aka jsplumb view)');
            return false;
        }
        if (this.state.href) {
            window.location.href = this.state.href;
        } else {
            this.createUriRule();
        }
    };
    
    createUriRule() {
        const rule = _.cloneDeep(this.props.ruleData);
        rule.rules.uriRule = {
            type: 'uri',
            pattern: '/',
        };
        updateObjectMappingAsync(rule)
            .subscribe(
                data => {
                    const href = getEditorHref(data.body.rules.uriRule.id);
                    if (href) {
                        window.location.href = href;
                    }
                },
                err => {
                    console.error(err);
                }
            );
        return false;
    }
    
    removeUriRule() {
        if (isDebugMode()) {
            event.stopPropagation();
            alert('Normally this would open the complex editor (aka jsplumb view)');
            return false;
        }
        
        const rule = _.cloneDeep(this.props.ruleData);
        const callbackFn = () => {
            rule.rules.uriRule = null;
            updateObjectMappingAsync(rule)
                .subscribe(
                    () => {
                        EventEmitter.emit(MESSAGES.RELOAD, true);
                    },
                    err => {
                        console.error(err);
                    }
                );
        };
        this.props.onClickedRemove(null, callbackFn);
        return false;
    };
    
    handleEdit() {
        this.setState({
            edit: !this.state.edit,
        });
    };
    
    handleCloseEdit = (obj) => {
        if (obj.id === this.props.ruleData.id) {
            this.setState({edit: false});
        }
    };
    
    handleCopy = () => {
        const {id, type} = this.props.ruleData;
        this.props.handleCopy(id, type);
    };
    
    handleClone = () => {
        const {id, type, parentId} = this.props.ruleData;
        this.props.handleClone(id, type, parentId);
    };
    
    render() {
        const {type, ruleData} = this.props;
        const {edit} = this.state;
        const {type: ruleType} = ruleData;
        
        if (edit) {
            return (
                <ObjectMappingRuleForm
                    id={this.props.ruleData.id}
                    parent={this.props.parent}
                    parentId={this.props.parentId}
                    ruleData={transformRuleOfObjectMapping(ruleData)}
                />
            );
        }
        
        // @FIXME type vs ruleType is it not same?
        return (
            <div>
                <div className="ecc-silk-mapping__rulesviewer">
                    <CardContent>
                        {
                            !isRootRule(type) ? [
                                <TargetProperty
                                    key={'ObjectTargetProperty'}
                                    mappingTargetUri={_.get(ruleData, 'mappingTarget.uri')}
                                />,
                                <ObjectEntityRelation
                                    key={'ObjectEntityRelation'}
                                    isBackwardProperty={_.get(ruleData, 'mappingTarget.isBackwardProperty')}
                                    parent={this.props.parent}
                                />
                            ] : null
                        }
                        {
                            _.get(ruleData, 'rules.typeRules[0].uri')
                                ? <ObjectTypeRules
                                    typeRules={_.get(ruleData, 'rules.typeRules') || {}}/>
                                : null
                        }
                        {
                            <ObjectUriPattern
                                uriRule={_.get(ruleData, 'rules.uriRule') || {}}
                                onRemoveUriRule={this.removeUriRule}
                                onEditUriRule={this.editUriRule}
                            />
                        }
                        {
                            isObjectRule(type) && ruleData.sourcePath
                                ? <ObjectSourcePath type={ruleData.type}>
                                    <SourcePath
                                        rule={{
                                            type,
                                            sourcePath: ruleData.sourcePath
                                        }}
                                    />
                                </ObjectSourcePath> : null
                        }
                        {
                            _.get(ruleData, 'rules.uriRule.id')
                                ? <ExampleTarget uriRuleId={_.get(ruleData, 'rules.uriRule.id')}/>
                                : null
                        }
                        {
                            _.get(ruleData, 'metadata.label')
                                ? <MetadataLabel label={_.get(ruleData, 'metadata.label', '')}/>
                                : null
                        }
                        {
                            _.get(ruleData, 'metadata.description')
                                ? <MetadataDesc description={_.get(ruleData, 'metadata.description', '')} />
                                : null
                        }
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__rulesviewer__actionrow">
                        <EditButton onEdit={this.handleEdit}/>
                        {isCopiableRule(ruleType) && <CopyButton onCopy={this.handleCopy}/>}
                        {isClonableRule(ruleType) && <CloneButton onClone={this.handleClone}/>}
                        {!isRootRule(type) && <DeleteButton
                            onDelete={() => {
                                this.props.onClickedRemove({
                                    id: this.props.ruleData.id,
                                    uri: this.props.ruleData.mappingTarget.uri,
                                    type: ruleType,
                                    parent: this.props.parentId
                                })
                            }}
                        />
                        }
                    </CardActions>
                </div>
            </div>
        );
    }
}

export default ObjectMappingRule;
