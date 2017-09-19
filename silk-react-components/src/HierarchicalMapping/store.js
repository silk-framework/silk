// Store specific to hierarchical mappings, will use silk-store internally

import _ from 'lodash';
import rxmq, {Rx} from 'ecc-messagebus';

const hierarchicalMappingChannel = rxmq.channel('silk.hierarchicalMapping');
const silkStore = rxmq.channel('silk.api');

// Set api details
let apiDetails = {
    transformTask: false,
    baseUrl: false,
    project: false,
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
                'The best suitable data type will be chosen automatically for each value',
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
                'Suited for XML Schema dates. Accepts values in the the following formats: xsd:date, xsd:gDay, xsd:gMonth, xsd:gMonthDay, xsd:gYear, xsd:gYearMonth.'
        },
        {
            value: 'DateTimeValueType',
            label: 'DateTime',
            description:
                'Suited for XML Schema dates and times. Accepts values in the the following formats: xsd:date, xsd:dateTime, xsd:gDay, xsd:gMonth, xsd:gMonthDay, xsd:gYear, xsd:gYearMonth, xsd:time.'
        }
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

    if (element.id === id) {
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
            !_.includes(['root', 'object'], result.type)
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

    return _.get(data, [path]);
};

const prepareValueMappingPayload = data => {
    const payload = {
        metadata: {
            description: data.comment,
        },
        mappingTarget: {
            uri: handleCreatedSelectBoxValue(data, 'targetProperty'),
            valueType: {
                nodeType: handleCreatedSelectBoxValue(data, 'propertyType'),
            },
        },
    };

    if (data.type === 'direct') {
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
        },
        mappingTarget: {
            uri: handleCreatedSelectBoxValue(data, 'targetProperty'),
            isBackwardProperty: data.entityConnection==='from',
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
                      type: 'uri',
                      pattern: data.pattern,
                  }
                : null,
            typeRules,
        },
    };

    if (!data.id) {
        payload.type = 'object';
        payload.rules.propertyRules = [];
    }

    return payload;
};

if (!__DEBUG__) {
    let rootId = null;

    const vocabularyCache = {};

    hierarchicalMappingChannel
        .subject('rules.generate')
        .subscribe(({data, replySubject}) => {
            const {correspondences, parentRuleId} = data;
            silkStore
                .request({
                    topic: 'transform.task.rule.generate',
                    data: {...apiDetails, correspondences, parentRuleId},
                })
                .map(returned => ({
                    rules: _.get(returned, ['body'], []),
                }))
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
            silkStore
                .request({
                    topic: 'transform.task.rule.suggestions',
                    data: {...apiDetails, ...data},
                })
                .map(returned => ({
                    suggestions: returned.body,
                }))
                .multicast(replySubject)
                .connect();
        });

    hierarchicalMappingChannel
        .subject('rule.child.example')
        .subscribe(({data, replySubject}) => {
            const {ruleType, rawRule, id} = data;
            if (id) {

                const rule = ruleType === "value"
                    ? prepareValueMappingPayload(rawRule)
                    : prepareObjectMappingPayload(rawRule)
                ;
                silkStore
                    .request({
                        topic: 'transform.task.rule.child.peak',
                        data: {...apiDetails, id, rule}
                    })
                    .map(returned => ({
                        example: returned.body,
                    }))
                    .multicast(replySubject)
                    .connect();
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
                    .map(returned => ({
                        example: returned.body,
                    }))
                    .multicast(replySubject)
                    .connect();
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
} else {
    // eslint-disable-next-line
  const rawMockStore = require("./retrieval2.json");

    let mockStore = null;

    try {
        mockStore = JSON.parse(localStorage.getItem('mockStore'));
    } catch (e) {
        console.warn('Could not load mockStore', e);
    }

    if (mockStore === null) {
        mockStore = _.cloneDeep(rawMockStore);
    }

    hierarchicalMappingChannel
        .subject('rules.generate')
        .subscribe(({data, replySubject}) => {
            const {correspondences} = data;

            const rules = [];

            _.map(correspondences, correspondence => {
                rules.push({
                    metadata: {
                        description: _.includes(
                            correspondence.sourcePath,
                            'error'
                        )
                            ? 'error'
                            : '',
                    },
                    mappingTarget: {
                        uri: correspondence.targetProperty,
                        valueType: {
                            nodeType: 'AutoDetectValueType',
                        },
                    },
                    sourcePath: correspondence.sourcePath,
                    type: 'direct',
                });
            });

            replySubject.onNext({rules});
            replySubject.onCompleted();
        });

    hierarchicalMappingChannel
        .subject('rule.suggestions')
        .subscribe(({data, replySubject}) => {
            const paths = [
                '/name',
                '/city',
                '/loan',
                '/country',
                '/lastname',
                '/firstName',
                '/address',
                '/expected-error',
            ];
            const types = [
                '/name',
                '/city',
                '/loan',
                '/country',
                '/lastname',
                '/firstName',
                '/address',
                '/one-error',
            ];
            const suggestions = {};
            _.forEach(data.targetClassUris, target => {
                _.forEach(types, (type, key) => {
                    const path = paths[key];
                    suggestions[`${target}${type}`] = [
                        {
                            uri: path,
                            confidence:
                                Math.floor(100 - 0.1 * Math.random() * 100) /
                                100,
                        },
                    ];
                });
            });

            replySubject.onNext({suggestions});
            replySubject.onCompleted();
        });

    hierarchicalMappingChannel
        .subject('autocomplete')
        .subscribe(({data, replySubject}) => {
            const {entity, input} = data;

            let result = [];

            switch (entity) {
                case 'propertyType':
                    filterPropertyType(input, replySubject);
                    return;
                case 'targetProperty':
                    result = [
                        {
                            value: 'http://xmlns.com/foaf/0.1/knows',
                            label: 'foaf:knows',
                            description:
                                'A person known by this person (indicating some level of reciprocated interaction between the parties).',
                        },
                        {
                            value: 'http://xmlns.com/foaf/0.1/name',
                            label: 'foaf:name',
                            description: 'A name for some thing.',
                        },
                        {
                            value: 'http://schmea.org/address',
                            label: 'schema:address',
                            description: 'Physical address of the item.',
                        },
                    ];
                    break;
                case 'targetEntityType':
                    result = [
                        {
                            value: 'http://xmlns.com/foaf/0.1/Person',
                            label: 'foaf:Person',
                            description:
                                "The Person class represents people. Something is a Person if it is a person. We don't nitpic about whether they're alive, dead, real, or imaginary. The Person class is a sub-class of the Agent class, since all people are considered 'agents' in FOAF.",
                        },
                        {
                            value: 'http://schema.org/PostalAddress',
                            label: 'schema:PostalAddress',
                            description: 'The mailing address.',
                        },
                    ];
                    break;
                case 'sourcePath':
                    result = [
                        {value: '/name', label: 'name'},
                        {value: '/address', label: 'address'},
                        {value: '/last_name', label: 'last name'},
                    ];
                    break;
                default:
                    if (__DEBUG__) {
                        console.error(`No autocomplete defined for ${entity}`);
                    }
            }

            const search = _.isString(input) ? input.toLocaleLowerCase() : '';

            replySubject.onNext({
                options: _.filter(
                    result,
                    ({value, label, description}) =>
                        _.includes(value.toLocaleLowerCase(), search) ||
                        _.includes(label.toLocaleLowerCase(), search) ||
                        _.includes(description.toLocaleLowerCase(), search)
                ),
            });

            replySubject.onCompleted();
        });

    hierarchicalMappingChannel
        .subject('hierarchy.get')
        .subscribe(({replySubject}) => {
            const hierarchy = _.chain(mockStore).value();

            replySubject.onNext({hierarchy});
            replySubject.onCompleted();
        });

    hierarchicalMappingChannel
        .subject('rule.child.example')
        .subscribe(({replySubject}) => {
            const example = {
                sourcePaths: [['/name'], ['/birthdate']],
                results: [
                    {
                        sourceValues: [['Abigale Purdy'], ['7/21/1977']],
                        transformedValues: ['abigale purdy7/21/1977'],
                    },
                    {
                        sourceValues: [['Ronny Wiegand'], ['10/24/1963']],
                        transformedValues: ['ronny wiegand10/24/1963'],
                    },
                    {
                        sourceValues: [['Rosalyn Wisozk'], ['5/8/1982']],
                        transformedValues: ['rosalyn wisozk5/8/1982'],
                    },
                ],
                status: {id: 'success', msg: ''},
            };
            replySubject.onNext({example});
            replySubject.onCompleted();
        });

    hierarchicalMappingChannel
        .subject('rule.example')
        .subscribe(({replySubject}) => {
            const example = {
                sourcePaths: [['/name'], ['/birthdate']],
                results: [
                    {
                        sourceValues: [['Abigale Purdy'], ['7/21/1977']],
                        transformedValues: ['abigale purdy7/21/1977'],
                    },
                    {
                        sourceValues: [['Ronny Wiegand'], ['10/24/1963']],
                        transformedValues: ['ronny wiegand10/24/1963'],
                    },
                    {
                        sourceValues: [['Rosalyn Wisozk'], ['5/8/1982']],
                        transformedValues: ['rosalyn wisozk5/8/1982'],
                    },
                ],
                status: {id: 'success', msg: ''},
            };
            replySubject.onNext({example});
            replySubject.onCompleted();
        });

    hierarchicalMappingChannel
        .subject('rule.get')
        .subscribe(({data, replySubject}) => {
            const {id, isObjectMapping = false} = data;
            const rule = findRule(
                _.cloneDeep(mockStore),
                id,
                isObjectMapping,
                []
            );
            const result = _.isNull(rule) ? mockStore : rule;
            replySubject.onNext({rule: result});
            replySubject.onCompleted();
        });

    const appendToMockStore = (store, id, payload) => {
        if (store.id === id && _.has(store, 'rules.propertyRules')) {
            store.rules.propertyRules.push(payload);
        } else if (store.id === id) {
            store.rules.propertyRules = [payload];
        } else if (_.has(store, 'rules.propertyRules')) {
            _.forEach(_.get(store, 'rules.propertyRules'), childRule => {
                appendToMockStore(childRule, id, payload);
            });
        }
    };

    const editRule = (store, id, payload) => {
        if (store.id === id) {
            if (
                _.has(store.rules, 'typeRules') &&
                _.has(payload.rules, 'typeRules')
            ) {
                store.rules.typeRules = payload.rules.typeRules;
            }
            _.merge(store, payload);
        } else if (_.has(store, 'rules.propertyRules')) {
            _.forEach(_.get(store, 'rules.propertyRules'), childRule => {
                editRule(childRule, id, payload);
            });
        }
    };

    const saveMockStore = () => {
        hierarchicalMappingChannel.subject('reload').onNext(true);
        localStorage.setItem('mockStore', JSON.stringify(mockStore));
    };

    const handleUpdatePreparedRule = ({data, replySubject}) => {
        const payload = data;

        if (_.includes(data.metadata.description, 'error')) {
            const err = new Error('Could not save rule.');
            _.set(err, 'response.body', {
                message: 'Comment cannot contain "error"',
                issues: [
                    {message: 'None really, we just want to test the feature'},
                ],
            });

            replySubject.onError(err);
            replySubject.onCompleted();
            return;
        }

        payload.id = `${Date.now()}${_.random(0, 100, false)}`;

        const parent = data.parentId ? data.parentId : mockStore.id;
        appendToMockStore(mockStore, parent, payload);

        saveMockStore();

        replySubject.onNext();
        replySubject.onCompleted();
    };

    const handleUpdate = ({data, replySubject}) => {
        const payload = _.includes(['object', 'root'], data.type)
            ? prepareObjectMappingPayload(data)
            : prepareValueMappingPayload(data);

        if (_.includes(data.comment, 'error')) {
            const err = new Error('Could not save rule.');
            _.set(err, 'response.body', {
                message: 'Comment cannot contain "error"',
                issues: [
                    {message: 'None really, we just want to test the feature'},
                ],
            });
            replySubject.onError(err);
            replySubject.onCompleted();
            return;
        }

        if (data.id) {
            editRule(mockStore, data.id, payload);
            saveMockStore();
        } else {
            payload.id = `${Date.now()}${_.random(0, 100, false)}`;

            const parent = data.parentId ? data.parentId : mockStore.id;
            appendToMockStore(mockStore, parent, payload);

            saveMockStore();
        }

        replySubject.onNext();
        replySubject.onCompleted();
    };

    hierarchicalMappingChannel
        .subject('rule.createValueMapping')
        .subscribe(handleUpdate);

    hierarchicalMappingChannel
        .subject('rule.createObjectMapping')
        .subscribe(handleUpdate);

    hierarchicalMappingChannel
        .subject('rule.createGeneratedMapping')
        .subscribe(handleUpdatePreparedRule);
    const removeRule = (store, id) => {
        if (store.id === id) {
            return null;
        } else if (_.has(store, 'rules.propertyRules')) {
            store.rules.propertyRules = _.filter(
                store.rules.propertyRules,
                v => removeRule(v, id) !== null
            );
        }
        return store;
    };

    hierarchicalMappingChannel
        .subject('rule.removeRule')
        .subscribe(({data, replySubject}) => {
            const {id} = data;
            mockStore = removeRule(_.chain(mockStore).value(), id);
            saveMockStore();
            replySubject.onNext();
            replySubject.onCompleted();
        });

    const orderRule = (store, id, pos) => {
        if (_.has(store, 'rules.propertyRules')) {
            const match = _.remove(
                store.rules.propertyRules,
                children => children.id === id
            );

            if (_.isEmpty(match)) {
                store.rules.propertyRules = _.map(
                    store.rules.propertyRules,
                    child => orderRule(child, id, pos)
                );
            } else {
                const spliceAt = _.max([
                    0,
                    _.min([pos, _.size(store.rules.propertyRules)]),
                ]);

                store.rules.propertyRules = [
                    ..._.slice(store.rules.propertyRules, 0, spliceAt),
                    ...match,
                    ..._.slice(store.rules.propertyRules, spliceAt),
                ];
            }
        }
        return store;
    };

    hierarchicalMappingChannel
        .subject('rule.orderRule')
        .subscribe(({data, replySubject}) => {
            const {pos, id} = data;
            mockStore = orderRule(_.chain(mockStore).value(), id, pos);
            saveMockStore();
            replySubject.onNext();
            replySubject.onCompleted();
        });

    // eslint-disable-next-line
  const loremIpsum = require("lorem-ipsum");

    hierarchicalMappingChannel
        .subject('vocabularyInfo.get')
        .subscribe(({data, replySubject}) => {
            const {field} = data;

            const ret = {info: null};

            switch (field) {
                case 'label':
                    break;
                case 'description':
                    ret.info = loremIpsum({
                        count: _.random(0, 2),
                        units: 'paragraphs',
                    });
                    break;
                default:
                    if (__DEBUG__) {
                        console.warn(
                            `No info for field ${field} available in mockStore`
                        );
                    }
            }

            replySubject.onNext(ret);
            replySubject.onCompleted();
        });
}

export default hierarchicalMappingChannel;
