// Store specific to hierarchical mappings, will use silk-store internally

import _ from 'lodash';
import rxmq, {Rx} from 'ecc-messagebus';
import {
    isObjectMappingRule,
    MAPPING_RULE_TYPE_DIRECT,
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_URI,
    MAPPING_RULE_TYPE_COMPLEX_URI,
    SUGGESTION_TYPES,
} from './helpers';
import {Suggestion} from './Suggestion';

const hierarchicalMappingChannel = rxmq.channel('silk.hierarchicalMapping');
const silkStore = rxmq.channel('silk.api');

// Set api details
let apiDetails = {
    transformTask: 'test',
    baseUrl: 'http://test.url',
    project: 'test',
};

// Set Api details
hierarchicalMappingChannel.subject('setSilkDetails').subscribe(data => {
    apiDetails = {...data};
});

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
        $search: _.deburr(
            `${datatype.value}|${datatype.label}|${datatype.description}`
        ).toLocaleLowerCase(),
    })
);

function filterPropertyType(input, replySubject) {
    const search = _.deburr(input).toLocaleLowerCase();

    replySubject.onNext({
        options: _.filter(datatypes, datatype =>
            _.includes(datatype.$search, search)
        ),
    });
    replySubject.onCompleted();
}

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
            !isObjectMappingRule(result.type)
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
    hierarchicalMappingChannel
        .request({
            topic: 'rule.createGeneratedMapping',
            data: {...rule, parentId},
        })
        .catch(e => Rx.Observable.return({error: e, rule}));

const createGeneratedRules = ({rules, parentId}) =>
    Rx.Observable.from(rules)
        .flatMapWithMaxConcurrent(5, rule =>
            Rx.Observable.defer(() => generateRule(rule, parentId))
        )
        .reduce((all, result, idx) => {
            const total = _.size(rules);
            const count = idx + 1;

            hierarchicalMappingChannel
                .subject('rule.suggestions.progress')
                .onNext({
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

let rootId = null;

const vocabularyCache = {};

hierarchicalMappingChannel
    .subject('rule.orderRule')
    .subscribe(({data, replySubject}) => {
        const {childrenRules, id} = data;
        silkStore
            .request({
                topic: 'transform.task.rule.rules.reorder',
                data: {id, childrenRules, ...apiDetails},
            })
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rules.generate')
    .subscribe(({data, replySubject}) => {
        const {correspondences, parentId} = data;
        silkStore
            .request({
                topic: 'transform.task.rule.generate',
                data: {...apiDetails, correspondences, parentId},
            })
            .map(returned => ({
                rules: _.get(returned, ['body'], []),
                parentId,
            }))
            .flatMap(createGeneratedRules)
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('vocabularyInfo.get')
    .subscribe(({data, replySubject}) => {
        const {uri, field} = data;

        const path = [uri, field];

        if (_.has(vocabularyCache, path)) {
            replySubject.onNext({
                info: _.get(vocabularyCache, path),
            });
            replySubject.onCompleted();
        } else {
            silkStore
                .request({
                    topic: 'transform.task.targetVocabulary.typeOrProperty',
                    data: {...apiDetails, uri},
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
                })
                .multicast(replySubject)
                .connect();
        }
    });

hierarchicalMappingChannel
    .subject('rule.suggestions')
    .subscribe(({data, replySubject}) => {
        Rx.Observable.forkJoin(
            silkStore
                .request({
                    // call the DI matchVocabularyClassDataset endpoint
                    topic: 'transform.task.rule.suggestions',
                    data: {...apiDetails, ...data},
                })
                .catch(err => {

                    // It comes always {title: "Not Found", detail: "Not Found"} when the endpoint is not found.
                    // see: SilkErrorHandler.scala
                    const errorBody = _.get(err, 'response.body')

                    if (err.status === 404 && errorBody.title === "Not Found" && errorBody.detail === "Not Found") {
                        return Rx.Observable.return(null);
                    }
                    errorBody.code = err.status;
                    return Rx.Observable.return({error: errorBody})
                })
                .map(returned => {
                    const body = _.get(returned, 'body', []);
                    const error = _.get(returned, 'error', []);

                    if (error) {
                        return {
                            error,
                        }
                    }
                    const suggestions = [];

                    _.forEach(body, (sources, sourcePathOrUri) => {
                        _.forEach(sources, ({uri: candidateUri, type, confidence}) => {
                            let mapFrom = sourcePathOrUri; // By default we map from the dataset to the vocabulary, which fits
                            let mapTo = candidateUri;
                            if(!data.matchFromDataset) {
                                mapFrom = candidateUri; // In this case the vocabulary is the source, so we have to switch direction
                                mapTo = sourcePathOrUri;
                            }
                            suggestions.push(
                                new Suggestion(
                                    mapFrom,
                                    type,
                                    mapTo,
                                    confidence
                                )
                            );
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
                    data: {unusedOnly: true, ...apiDetails, ...data},
                })
                .catch(err => {
                    const errorBody = _.get(err, 'response.body');
                    errorBody.code = err.status;
                    return Rx.Observable.return({error: errorBody});

                })
                .map(returned => {
                    const body = _.get(returned, 'body', []);
                    const error = _.get(returned, 'error', []);
                    if (error) {
                        return {
                            error,
                        }
                    }
                    return {
                        data: _.map(body, path => new Suggestion(path))
                    }
                }),
            (arg1, arg2) => {
                return {
                    suggestions: _.filter(_.concat([], arg1.data, arg2.data), d => !_.isUndefined(d)),
                    warnings: _.filter([arg1.error, arg2.error], e => !_.isUndefined(e))
                };
            }
        )
            .multicast(replySubject)
            .connect();
    });

function mapPeakResult(returned) {
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
}

hierarchicalMappingChannel
    .subject('rule.child.example')
    .subscribe(({data, replySubject}) => {
        const {ruleType, rawRule, id} = data;
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
                    throw new Error(
                        'Rule send to rule.child.example type must be in ("value","object","uri","complexURI")'
                    );
            }
        };
        const rule = getRule(rawRule, ruleType);
        if (rule && id) {
            silkStore
                .request({
                    topic: 'transform.task.rule.child.peak',
                    data: {...apiDetails, id, rule},
                })
                .subscribe(returned => {
                    const result = mapPeakResult(returned);
                    if (result.title) {
                        replySubject.onError(result);
                    } else {
                        replySubject.onNext(result);
                    }
                    replySubject.onCompleted();
                });
        }
    });

hierarchicalMappingChannel
    .subject('rule.example')
    .subscribe(({data, replySubject}) => {
        const {id} = data;
        if (id) {
            silkStore
                .request({
                    topic: 'transform.task.rule.peak',
                    data: {...apiDetails, id},
                })
                .subscribe(returned => {
                    const result = mapPeakResult(returned);
                    if (result.title) {
                        replySubject.onError(result);
                    } else {
                        replySubject.onNext(result);
                    }
                    replySubject.onCompleted();
                });
        }
    });

hierarchicalMappingChannel
    .subject('hierarchy.get')
    .subscribe(({replySubject}) => {
        silkStore
            .request({
                topic: 'transform.task.rules.get',
                data: {...apiDetails},
            })
            .map(returned => {
                const rules = returned.body;

                if (!_.isString(rootId)) {
                    rootId = rules.id;
                }

                return {
                    hierarchy: rules,
                };
            })
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rule.getEditorHref')
    .subscribe(({data, replySubject}) => {
        const {id: ruleId} = data;

        if (ruleId) {
            const {transformTask, baseUrl, project} = apiDetails;

            replySubject.onNext({
                href: `${baseUrl}/transform/${project}/${transformTask}/editor/${ruleId}`,
            });
        } else {
            replySubject.onNext({
                href: null,
            });
        }

        replySubject.onCompleted();
    });

hierarchicalMappingChannel
    .subject('rule.get')
    .subscribe(({data, replySubject}) => {
        const {id, isObjectMapping} = data;

        silkStore
            .request({
                topic: 'transform.task.rules.get',
                data: {...apiDetails},
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

                return {rule: rule || rules};
            })
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('autocomplete')
    .subscribe(({data, replySubject}) => {
        const {entity, input, ruleId = rootId} = data;

        let channel = 'transform.task.rule.completions.';

        switch (entity) {
            case 'propertyType':
                filterPropertyType(input, replySubject);
                return;
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

        silkStore
            .request({
                topic: channel,
                data: {...apiDetails, term: input, ruleId},
            })
            .map(returned => ({options: returned.body}))
            .multicast(replySubject)
            .connect();
    });

const editMappingRule = (payload, id, parent) => {
    if (id) {
        return silkStore.request({
            topic: 'transform.task.rule.put',
            data: {
                ...apiDetails,
                ruleId: id,
                payload,
            },
        });
    }

    return silkStore.request({
        topic: 'transform.task.rule.rules.append',
        data: {
            ...apiDetails,
            ruleId: parent,
            payload,
        },
    });
};

hierarchicalMappingChannel
    .subject('rule.createValueMapping')
    .subscribe(({data, replySubject}) => {
        const payload = prepareValueMappingPayload(data);
        const parent = data.parentId ? data.parentId : rootId;

        editMappingRule(payload, data.id, parent)
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rule.createObjectMapping')
    .subscribe(({data, replySubject}) => {
        const payload = prepareObjectMappingPayload(data);
        const parent = data.parentId ? data.parentId : rootId;

        editMappingRule(payload, data.id, parent)
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rule.updateObjectMapping')
    .subscribe(({data, replySubject}) => {
        editMappingRule(data, data.id, parent)
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rule.createGeneratedMapping')
    .subscribe(({data, replySubject}) => {
        const payload = data;
        const parent = data.parentId ? data.parentId : rootId;

        editMappingRule(payload, false, parent)
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rule.removeRule')
    .subscribe(({data, replySubject}) => {
        const {id} = data;
        silkStore
            .request({
                topic: 'transform.task.rule.delete',
                data: {
                    ...apiDetails,
                    ruleId: id,
                },
            })
            .subscribe(
                () => {
                    replySubject.onNext();
                    replySubject.onCompleted();
                    hierarchicalMappingChannel
                        .subject('reload')
                        .onNext(true);
                },
                err => {
                    // TODO: Beautify
                }
            );
    });

hierarchicalMappingChannel
    .subject('rule.getDataToCopyRule')
    .subscribe(({data, replySubject}) => {
        const {id, isObjectMapping} = data;

        silkStore
            .request({
                topic: 'transform.task.rules.get',
                data: {...apiDetails},
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

                let data = {
                    type: rule.type,
                    comment: _.get(rule, 'metadata.description', ''),
                    label: _.get(rule, 'metadata.label', ''),
                    targetProperty: _.get(rule, 'mappingTarget.uri', undefined),
                    sourceProperty: _.get(rule, 'sourcePath', undefined),
                };

                if (rule.type === MAPPING_RULE_TYPE_DIRECT) {
                    data.valueType = _.get(rule, 'mappingTarget.valueType', {nodeType: 'StringValueType'});
                    data.isAttribute = _.get(rule, 'mappingTarget.isAttribute', false);
                } else if (rule.type === MAPPING_RULE_TYPE_COMPLEX) {
                    data.valueType = _.get(rule, 'mappingTarget.valueType', {nodeType: 'StringValueType'});
                    data.isAttribute = _.get(rule, 'mappingTarget.isAttribute', false);
                    data.operator = rule.operator;
                    data.sourcePaths = rule.sourcePaths;
                } else if (rule.type === MAPPING_RULE_TYPE_OBJECT) {
                    data.targetEntityType = _.chain(rule)
                        .get('rules.typeRules', [])
                        .map('typeUri')
                        .value();
                    data.pattern = _.get(rule, 'rules.uriRule.pattern', '');
                    data.entityConnection = _.get(
                        rule,
                        'mappingTarget.isBackwardProperty',
                        false
                    );
                    data.copiedObjectId = id;
                    data.copiedObjectTask = apiDetails.transformTask;
                    data.copiedObjectProject = apiDetails.project;
                };

                return {data: data};
            })
            .multicast(replySubject)
            .connect();
    });

hierarchicalMappingChannel
    .subject('rule.copyObjectMapping')
    .subscribe(({data, replySubject}) => {
        const copiedDetails = {
            transformTask: data.copiedObjectTask,
            baseUrl: apiDetails.baseUrl,
            project: data.copiedObjectProject,
        };
        const parent = data.parentId ? data.parentId : rootId;

        silkStore
            .request({
                topic: 'transform.task.rules.get',
                data: {...copiedDetails},
            })
            .subscribe(returned => {
                function removeKeys(obj, keys) {
                    var index;
                    for (var prop in obj) {
                        if (obj.hasOwnProperty(prop)) {
                            switch (typeof(obj[prop])) {
                                case 'string':
                                    index = keys.indexOf(prop);
                                    if(index > -1)
                                        delete obj[prop]
                                    break;
                                case 'object':
                                    index = keys.indexOf(prop);
                                    if (index > -1)
                                        delete obj[prop]
                                    else
                                        removeKeys(obj[prop], keys)
                                    break;
                            }
                        }
                    }
                }

                const rules = returned.body;
                if (!_.isString(rootId))
                    rootId = rules.id;
                let rule = findRule(
                    _.cloneDeep(rules),
                    data.copiedObjectId,
                    true,
                    []
                );
                removeKeys(rule, 'id');
                rule.metadata.label = 'Copy of ' + rule.metadata.label;
                silkStore
                    .request({
                        topic: 'transform.task.rule.rules.append',
                        data: {
                            ...apiDetails,
                            ruleId: parent,
                            payload: rule,
                        },
                    })
                    .multicast(replySubject)
                    .connect();
            })
    })

export default hierarchicalMappingChannel;
