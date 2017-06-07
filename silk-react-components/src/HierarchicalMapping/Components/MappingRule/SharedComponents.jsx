import React from 'react';
import _ from 'lodash';

export const RuleTitle = ({rule}) => {

    switch (rule.type) {
    case 'root':
        const title = _.get(rule, 'rules.typeRules[0].typeUri', '(no target type)');
        return (<span>{_.isEmpty(title) ? '[no type]' : title}</span>);
    case 'direct':
    case 'object':
        return (<span>{_.get(rule, 'mappingTarget.uri', '(no target property)')}</span>);
    case 'complex':
        //TODO: Complex Mappings need better titles
        return 'Complex Mapping';
        return <span>Complex Mapping</span>
    }

};

export const RuleTypes = ({rule}) => {

    switch (rule.type) {
    case 'object':
        let types = _.get(rule, 'rules.typeRules', []).map(({typeUri}) => typeUri);
        types = _.isEmpty(types) ? '(no target type)' : types.join(', ');
        return <span>{types}</span>;
    case 'root':
    case 'direct':
    case 'complex':
        return <span/>;
    }

};

export const SourcePath = ({rule}) => {
    const path = _.get(rule, 'sourcePath', '(no source path)');

    return <span>{_.isArray(path) ? path.join(', ') : path}</span>;

};

export const RuleTreeTitle = ({rule}) => {
    const childCount = _.get(rule, 'rules.propertyRules', []).length;
    switch (rule.type) {
        case 'root':
            const title = _.get(rule, 'rules.typeRules[0].typeUri', '(no target type)');
            return (<span>{_.isEmpty(title) ? '[no type]' : title} ({childCount})</span>);
        case 'object':
            return (<span>{_.get(rule, 'mappingTarget.uri', '(no target property)')} ({childCount})</span>);
        default:
            return false;
    }

};

export const RuleTreeTypes = ({rule}) => {

    switch (rule.type) {
        case 'object':
            let types = _.get(rule, 'rules.typeRules', []).map(({typeUri}) => typeUri);
            types = _.isEmpty(types) ? '(no target type)' : types.join(', ');
            return <span>{types}</span>;
        case 'root':
        default:
            return false;
    }

};
