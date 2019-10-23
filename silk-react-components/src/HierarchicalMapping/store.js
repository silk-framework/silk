// Store specific to hierarchical mappings, will use silk-store internally

import _ from 'lodash';
import rxmq, { Rx } from 'ecc-messagebus';
import {
    MAPPING_RULE_TYPE_ROOT,
    } from './utils/constants';

import { Suggestion } from './utils/Suggestion';
import {
    isRootOrObjectRule,
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_COMPLEX_URI, MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_URI,
    MESSAGES
} from './utils/constants';
import EventEmitter from './utils/EventEmitter';

const silkStore = rxmq.channel('silk.api');
export const errorChannel = rxmq.channel('errors');

let rootId = null;

const vocabularyCache = {};

const datatypes = _.map(
    [
        {
            value: 'AutoDetectValueType',
            label: 'Auto Detect',
            description:
                'The data type is decided automatically, based on the lexical form of each value.',
        },
        {
            value: 'UriValueType',
            label: 'URI',
            description:
                'Suited for values which are Unique Resource Identifiers',
        },
        {
            value: 'BooleanValueType',
            label: 'Boolean',
            description: 'Suited for values which are either true or false',
        },
        {
            value: 'StringValueType',
            label: 'String',
            description: 'Suited for values which contain text',
        },
        {
            value: 'IntegerValueType',
            label: 'Integer',
            description: 'Suited for numbers which have no fractional value',
        },
        {
            value: 'FloatValueType',
            label: 'Float',
            description: 'Suited for numbers which have a fractional value',
        },
        {
            value: 'LongValueType',
            label: 'Long',
            description:
                'Suited for large numbers which have no fractional value',
        },
        {
            value: 'DoubleValueType',
            label: 'Double',
            description:
                'Suited for large numbers which have a fractional value',
        },
        {
            value: 'DateValueType',
            label: 'Date',
            description:
                'Suited for XML Schema dates. Accepts values in the the following formats: xsd:date, xsd:gDay, xsd:gMonth, xsd:gMonthDay, xsd:gYear, xsd:gYearMonth.',
        },
        {
            value: 'DateTimeValueType',
            label: 'DateTime',
            description:
                'Suited for XML Schema dates and times. Accepts values in the the following formats: xsd:date, xsd:dateTime, xsd:gDay, xsd:gMonth, xsd:gMonthDay, xsd:gYear, xsd:gYearMonth, xsd:time.',
        },
        {
            value: 'LanguageValueType',
            label: 'Language Tagged',
            description:
                'Suited for texts that are in a specific language.',
        },
    ],
    datatype => ({
        ...datatype,
        $search: _.deburr(`${datatype.value}|${datatype.label}|${datatype.description}`).toLocaleLowerCase(),
    })
);

let _apiDetails = {};
export const setApiDetails = data => {
    _apiDetails = { ...data };
};
export const getApiDetails = () => _apiDetails;

const mapPeakResult = (returned) => {
    if (_.get(returned, 'body.status.id') !== 'success') {
        return {
            title: 'Could not load preview',
            detail: _.get(
                returned,
                'body.status.msg',
                'No details available'
            ),
        };
    }

    return {
        example: returned.body,
    };
};

const editMappingRule = (payload, id, parent) => {
    if (id) {
        return silkStore.request({
            topic: 'transform.task.rule.put',
            data: {
                ...getApiDetails(),
                ruleId: id,
                payload,
            },
        });
    }

    return silkStore.request({
        topic: 'transform.task.rule.rules.append',
        data: {
            ...getApiDetails(),
            ruleId: parent,
            payload,
        },
    });
};

function findRule(curr, id, isObjectMapping, breadcrumbs) {
    const element = {
        ...curr,
        breadcrumbs,
    };

    if (element.id === id || _.get(element, 'rules.uriRule.id') === id) {
        return element;
    } else if (_.has(element, 'rules.propertyRules')) {
        let result = null;
        const bc = [
            ...breadcrumbs,
            {
                id: element.id,
                type: _.get(element, 'rules.typeRules[0].typeUri', false),
                property: _.get(element, 'mappingTarget.uri', false),
            },
        ];
        _.forEach(element.rules.propertyRules, child => {
            if (result === null) {
                result = findRule(child, id, isObjectMapping, bc);
            }
        });

        if (
            isObjectMapping &&
            result !== null &&
            !isRootOrObjectRule(result.type)
        ) {
            result = element;
        }

        return result;
    }
    return null;
}

const handleCreatedSelectBoxValue = (data, path) => {
    if (_.has(data, [path, 'value'])) {
        return _.get(data, [path, 'value']);
    }
    // the select boxes return an empty array when the user delete the existing text,
    // instead of returning an empty string
    if (_.isEmpty(_.get(data, [path]))) {
        return '';
    }

    return _.get(data, [path]);
};

const prepareValueMappingPayload = data => {
    const payload = {
        metadata: {
            description: data.comment,
            label: data.label,
        },
        mappingTarget: {
            uri: handleCreatedSelectBoxValue(data, 'targetProperty'),
            valueType: handleCreatedSelectBoxValue(data, 'valueType'),
            isAttribute: data.isAttribute,
        },
    };

    if (data.type === MAPPING_RULE_TYPE_DIRECT) {
        payload.sourcePath = data.sourceProperty
            ? handleCreatedSelectBoxValue(data, 'sourceProperty')
            : '';
    }

    if (!data.id) {
        payload.type = data.type;
    }

    return payload;
};

const prepareObjectMappingPayload = data => {
    const typeRules = _.map(data.targetEntityType, typeRule => {
        const value = _.get(typeRule, 'value', typeRule);

        return {
            type: 'type',
            typeUri: value,
        };
    });

    const payload = {
        metadata: {
            description: data.comment,
            label: data.label,
        },
        mappingTarget: {
            uri: handleCreatedSelectBoxValue(data, 'targetProperty'),
            isBackwardProperty: data.entityConnection,
            valueType: {
                nodeType: 'UriValueType',
            },
        },
        sourcePath: data.sourceProperty
            ? handleCreatedSelectBoxValue(data, 'sourceProperty')
            : '',
        rules: {
            uriRule: data.pattern
                ? {
                    type: MAPPING_RULE_TYPE_URI,
                    pattern: data.pattern,
                }
                : undefined,
            typeRules,
        },
    };

    if (!data.id) {
        payload.type = MAPPING_RULE_TYPE_OBJECT;
        payload.rules.propertyRules = [];
    }

    return payload;
};

const generateRule = (rule, parentId) =>
    createGeneratedMappingAsync({
        ...rule,
        parentId,
    }).catch(e => Rx.Observable.return({ error: e, rule }));

const createGeneratedRules = ({ rules, parentId }) =>
    Rx.Observable.from(rules)
        .flatMapWithMaxConcurrent(5, rule =>
            Rx.Observable.defer(() => generateRule(rule, parentId)))
        .reduce((all, result, idx) => {
            const total = _.size(rules);
            const count = idx + 1;
            EventEmitter.emit(MESSAGES.RULE.SUGGESTIONS.PROGRESS, {
                progressNumber: _.round(count / total * 100, 0),
                lastUpdate: `Saved ${count} of ${total} rules.`,
            });

            all.push(result);

            return all;
        }, [])
        .map(createdRules => {
            const failedRules = _.filter(createdRules, 'error');

            if (_.size(failedRules)) {
                const error = new Error('Could not create rules.');
                error.failedRules = failedRules;
                throw error;
            }
            return createdRules;
        });

// PUBLIC API
export const orderRulesAsync = ({ id, childrenRules }) => {
    silkStore
        .request({
            topic: 'transform.task.rule.rules.reorder',
            data: { id, childrenRules, ...getApiDetails() },
        })
        .map(() => {
            EventEmitter.emit(MESSAGES.RELOAD);
        });
};

export const generateRuleAsync = (correspondences, parentId) => {
    return silkStore
        .request({
            topic: 'transform.task.rule.generate',
            data: { ...getApiDetails(), correspondences, parentId },
        })
        .map(returned => {
            return {
                rules: _.get(returned, ['body'], []),
                parentId,
            };
        })
        .flatMap(createGeneratedRules)
        .map(() => {
            EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id: 0 });
            EventEmitter.emit(MESSAGES.RELOAD, true);
        });
};

export const getVocabInfoAsync = (uri, field) => {
    const path = [uri, field];

    if (_.has(vocabularyCache, path)) {
        return Rx.Observable.just({
            info: _.get(vocabularyCache, path),
        });
    }
    return silkStore
        .request({
            topic: 'transform.task.targetVocabulary.typeOrProperty',
            data: { ...getApiDetails(), uri },
        })
        .catch(() => Rx.Observable.just({}))
        .map(returned => {
            const info = _.get(
                returned,
                ['body', 'genericInfo', field],
                null
            );

            _.set(vocabularyCache, path, info);

            return {
                info,
            };
        });
};

export const getSuggestionsAsync = data => {
    return Rx.Observable.forkJoin(
        silkStore
            .request({
                // call the DI matchVocabularyClassDataset endpoint
                topic: 'transform.task.rule.suggestions',
                data: { ...getApiDetails(), ...data },
            })
            .catch(err => {
                // It comes always {title: "Not Found", detail: "Not Found"} when the endpoint is not found.
                // see: SilkErrorHandler.scala
                const errorBody = _.get(err, 'response.body');

                if (err.status === 404 && errorBody.title === 'Not Found' && errorBody.detail === 'Not Found') {
                    return Rx.Observable.return(null);
                }
                errorBody.code = err.status;
                return Rx.Observable.return({ error: errorBody });
            })
            .map(returned => {
                const body = _.get(returned, 'body', []);
                const error = _.get(returned, 'error', []);

                if (error) {
                    return {
                        error,
                    };
                }
                const suggestions = [];

                _.forEach(body, (sources, sourcePathOrUri) => {
                    _.forEach(sources, ({ uri: candidateUri, type, confidence }) => {
                        let mapFrom = sourcePathOrUri; // By default we map from the dataset to the vocabulary, which fits
                        let mapTo = candidateUri;
                        if (!data.matchFromDataset) {
                            mapFrom = candidateUri; // In this case the vocabulary is the source, so we have to switch direction
                            mapTo = sourcePathOrUri;
                        }
                        suggestions.push(new Suggestion(
                            mapFrom,
                            type,
                            mapTo,
                            confidence
                        ));
                    });
                });
                return {
                    data: suggestions,
                };
            }),
        silkStore
            .request({
                // call the silk endpoint valueSourcePaths
                topic: 'transform.task.rule.valueSourcePaths',
                data: { unusedOnly: true, ...getApiDetails(), ...data },
            })
            .catch(err => {
                const errorBody = _.get(err, 'response.body');
                errorBody.code = err.status;
                return Rx.Observable.return({ error: errorBody });
            })
            .map(returned => {
                const body = _.get(returned, 'body', []);
                const error = _.get(returned, 'error', []);
                if (error) {
                    return {
                        error,
                    };
                }
                return {
                    data: _.map(body, path => new Suggestion(path)),
                };
            }),
        (arg1, arg2) => {
            return {
                suggestions: _.filter(_.concat([], arg1.data, arg2.data), d => !_.isUndefined(d)),
                warnings: _.filter([arg1.error, arg2.error], e => !_.isUndefined(e)),
            };
        }
    );
};

export const childExampleAsync = data => {
    const { ruleType, rawRule, id } = data;
    const getRule = (rawRule, type) => {
        switch (type) {
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            return prepareValueMappingPayload(rawRule);
        case MAPPING_RULE_TYPE_OBJECT:
            return prepareObjectMappingPayload(rawRule);
        case MAPPING_RULE_TYPE_URI:
        case MAPPING_RULE_TYPE_COMPLEX_URI:
            return rawRule;
        default:
            throw new Error('Rule send to rule.child.example type must be in ("value","object","uri","complexURI")');
        }
    };

    const rule = getRule(rawRule, ruleType);

    if (rule && id) {
        return silkStore
            .request({
                topic: 'transform.task.rule.child.peak',
                data: { ...getApiDetails(), id, rule },
            })
            .map(mapPeakResult);
    }

    return Rx.Observable();
};

export const ruleExampleAsync = data => {
    const { id } = data;
    if (id) {
        return silkStore
            .request({
                topic: 'transform.task.rule.peak',
                data: { ...getApiDetails(), id },
            })
            .map(mapPeakResult);
    }
    return Rx.Observable();
};

export const getHierarchyAsync = () => {
    return silkStore
        .request({
            topic: 'transform.task.rules.get',
            data: {
                ...getApiDetails(),
            },
        })
        .map(returned => {
            const rules = returned.body;

            if (!_.isString(rootId)) {
                rootId = rules.id;
            }

            return {
                hierarchy: rules,
            };
        });
};

export const getEditorHref = ruleId => {
    const { transformTask, baseUrl, project } = getApiDetails();
    return ruleId ? `${baseUrl}/transform/${project}/${transformTask}/editor/${ruleId}` : null;
};

export const getRuleAsync = (id, isObjectMapping = false) => {
    return silkStore
        .request({
            topic: 'transform.task.rules.get',
            data: { ...getApiDetails() },
        })
        .map(returned => {
            const rules = returned.body;
            const searchId = id || rules.id;
            if (!_.isString(rootId)) {
                rootId = rules.id;
            }
            const rule = findRule(
                _.cloneDeep(rules),
                searchId,
                isObjectMapping,
                []
            );
            return { rule: rule || rules };
        });
};

export const autocompleteAsync = data => {
    const { entity, input, ruleId = rootId } = data;

    let channel = 'transform.task.rule.completions.';
    switch (entity) {
    case 'propertyType':
        const search = _.deburr(input).toLocaleLowerCase();
        return Rx.Observable.just({
            options: _.filter(datatypes, datatype =>
                _.includes(datatype.$search, search)),
        });
    case 'targetProperty':
        channel += 'targetProperties';
        break;
    case 'targetEntityType':
        channel += 'targetTypes';
        break;
    case 'sourcePath':
        channel += 'sourcePaths';
        break;
    default:
        if (__DEBUG__) {
            console.error(`No autocomplete defined for ${entity}`);
        }
    }

    return silkStore
        .request({
            topic: channel,
            data: { ...getApiDetails(), term: input, ruleId },
        })
        .map(returned => ({ options: returned.body }));
};

export const createMappingAsync = (data, isObject = false) => {
    const payload = isObject ? prepareObjectMappingPayload(data) : prepareValueMappingPayload(data);
    return editMappingRule(payload, data.id, data.parentId || rootId);
};

export const updateObjectMappingAsync = data => {
    return editMappingRule(data, data.id, data.parentId || rootId);
};

export const createGeneratedMappingAsync = data => {
    return editMappingRule(data, false, data.parentId || rootId);
};

export const ruleRemoveAsync = id => {
    return silkStore
        .request({
            topic: 'transform.task.rule.delete',
            data: {
                ...getApiDetails(),
                ruleId: id,
            },
        })
        .map(
            () => {
                EventEmitter.emit(MESSAGES.RELOAD, true);
            },
            err => {
                // TODO: Beautify
            }
        );
};

export const copyRuleAsync = data => {
    const { baseUrl, project, transformTask } = getApiDetails();
    return silkStore
        .request({
            topic: 'transform.task.rule.copy',
            data: {
                baseUrl,
                project,
                transformTask,
                id: data.id || MAPPING_RULE_TYPE_ROOT,
                queryParameters: data.queryParameters,
                appendTo: data.id, // the rule the copied rule should be appended to
            },
        })
        .map(returned => returned.body.id);
};
