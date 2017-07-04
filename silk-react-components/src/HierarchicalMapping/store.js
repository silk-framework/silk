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
hierarchicalMappingChannel.subject('setSilkDetails').subscribe((data) => {
    apiDetails = {...data};
});

const datatypes = _.map(
    [
        {
            value: "AutoDetectValueType",
            label: "Auto Detect",
            description: "The best suitable data type will be chosen automatically"
        },
        {value: "UriValueType", label: "URI", description: "Suited for values which are Unique Resource Identifiers"},
        {value: "BooleanValueType", label: "Boolean", description: "Suited for values which are either true or false"},
        {value: "StringValueType", label: "String", description: "Suited for values which contain text"},
        {value: "IntegerValueType", label: "Integer", description: "Suited for numbers which have no fractional value"},
        {value: "FloatValueType", label: "Float", description: "Suited for numbers which have a fractional value"},
        {value: "LongValueType", label: "Long", description: "Suited for large numbers which have no fractional value"},
        {
            value: "DoubleValueType",
            label: "Double",
            description: "Suited for large numbers which have a fractional value"
        },
    ],
    (datatype) => {
        datatype.$search = _.deburr(`${datatype.value}|${datatype.label}|${datatype.description}`).toLocaleLowerCase()
        return datatype;
    });

function filterPropertyType(input, replySubject) {

    const search = _.deburr(input).toLocaleLowerCase();

    replySubject.onNext({
            options: _.filter(datatypes, (datatype) => _.includes(datatype.$search, search))
        }
    );
    replySubject.onCompleted();
}


function findRule(element, id, breadcrumbs) {
    element.breadcrumbs = breadcrumbs;
    if (element.id === id) {
        return element;
    } else if (_.has(element, 'rules.propertyRules')) {
        let result = null;
        const bc = [
            ...breadcrumbs,
            {
                id: element.id,
                type: _.get(element, 'rules.typeRules[0].typeUri', false),
                property: _.get(element, 'mappingTarget.uri', false)
            }
        ];
        _.forEach(element.rules.propertyRules, (child) => {
            if (result === null)
                result = findRule(child, id, bc);
        });

        return result;
    }
    return null;
}
const handleCreatedSelectBoxValue = (data, path) => {

    if (_.has(data, [path, 'value'])) {
        return _.get(data, [path, 'value'])
    }

    return _.get(data, [path]);

};

const prepareValueMappingPayload = (data) => {

    const payload = {
        "metadata": {
            description: data.comment,
        },
        "mappingTarget": {
            "uri": handleCreatedSelectBoxValue(data, 'targetProperty'),
            "valueType": {
                "nodeType": handleCreatedSelectBoxValue(data, 'propertyType'),
            }
        }
    };

    if (data.type === 'direct') {
        payload.sourcePath = data.sourceProperty ? handleCreatedSelectBoxValue(data, 'sourceProperty') : '';
    }

    if (!data.id) {
        payload.type = data.type;
    }

    return payload;
};


const prepareObjectMappingPayload = (data) => {

    const typeRules = _.map(data.targetEntityType, (typeRule) => {

        const value = _.get(typeRule, 'value', typeRule);

        return {
            "type": "type",
            "typeUri": value,
        }
    });

    const payload = {
        "metadata": {
            description: data.comment,
        },
        "mappingTarget": {
            "uri": handleCreatedSelectBoxValue(data, 'targetProperty'),
            "isBackwardProperty": data.entityConnection,
            "valueType": {
                "nodeType": "UriValueType",
            }
        },
        sourcePath: data.sourceProperty ? handleCreatedSelectBoxValue(data, 'sourceProperty') : '',
        "rules": {
            "uriRule": data.pattern ? {
                "type": "uri",
                "pattern": data.pattern
            } : null,
            "typeRules": typeRules,
        }
    };

    if (!data.id) {
        payload.type = 'object';
        payload.rules.propertyRules = [];
    }

    return payload;
};

if (!__DEBUG__) {

    const rootId = 'root';

    const vocabularyCache = {};

    hierarchicalMappingChannel.subject('rules.generate').subscribe(
        ({data, replySubject}) => {
            const {correspondences, parentRuleId} = data;
            silkStore
                .request({topic: 'transform.task.rule.generate', data: {...apiDetails, correspondences, parentRuleId}})
                .map((returned) => {
                    return {
                        rules: _.get(returned, ['body'], []),
                    }
                }).multicast(replySubject).connect();


        }
    );

    hierarchicalMappingChannel.subject('vocabularyInfo.get').subscribe(
        ({data, replySubject}) => {

            const {uri, field} = data;

            const path = [uri, field];

            if (_.has(vocabularyCache, path)) {
                replySubject.onNext({
                    info: _.get(vocabularyCache, path)
                });
                replySubject.onCompleted()
            } else {

                silkStore
                    .request({topic: 'transform.task.targetVocabulary.type', data: {...apiDetails, uri}})
                    .catch((e) => {
                        return silkStore.request({
                            topic: 'transform.task.targetVocabulary.property',
                            data: {...apiDetails, uri}
                        }).catch(() => Rx.Observable.just({}))
                    })
                    .map((returned) => {

                        const info = _.get(returned, ['body', 'genericInfo', field], null);

                        _.set(vocabularyCache, path, info);

                        return {
                            info,
                        };
                    }).multicast(replySubject).connect();
            }
        }
    );

    hierarchicalMappingChannel.subject('rule.suggestions').subscribe(
        ({data, replySubject}) => {
            silkStore
                .request({topic: 'transform.task.rule.suggestions', data: {...apiDetails, ...data}}).map((returned) => {
                return {
                    suggestions: returned.body
                };
            }).multicast(replySubject).connect();
        }
    );

    hierarchicalMappingChannel.subject('rule.example').subscribe(
        ({data, replySubject}) => {

            const {id} = data;
            if (id) {
                silkStore
                    .request({topic: 'transform.task.rule.peak', data: {...apiDetails, id}}).map((returned) => {
                    return {
                        example: returned.body
                    };
                })
                    .multicast(replySubject).connect();
            }

        }
    );

    hierarchicalMappingChannel.subject('hierarchy.get').subscribe(
        ({data, replySubject}) => {

            silkStore
                .request({topic: 'transform.task.rules.get', data: {...apiDetails}})
                .map((returned) => {
                    return {
                        hierarchy: returned.body
                    };
                })
                .multicast(replySubject).connect();

        }
    );

    hierarchicalMappingChannel.subject('rule.getEditorHref').subscribe(
        ({data, replySubject}) => {

            const {id: ruleId} = data;

            if (ruleId) {
                const {
                    transformTask,
                    baseUrl,
                    project,
                } = apiDetails;

                replySubject.onNext({
                    href: `${baseUrl}/transform/${project}/${transformTask}/editor/${ruleId}`
                });
            } else {
                replySubject.onNext({
                    href: null
                });
            }

            replySubject.onCompleted();

        }
    );

    hierarchicalMappingChannel.subject('rule.get').subscribe(
        ({data, replySubject}) => {

            const {id} = data;

            silkStore
                .request({topic: 'transform.task.rules.get', data: {...apiDetails}})
                .map((returned) => {

                    const mockStore = returned.body;

                    const searchId = id ? id : mockStore.id;


                    const rule = findRule(_.cloneDeep(mockStore), searchId, []);


                    return {rule: rule ? rule : mockStore};
                })
                .multicast(replySubject).connect();


        }
    );

    hierarchicalMappingChannel.subject('autocomplete').subscribe(({data, replySubject}) => {

        const {entity, input, ruleId} = data;

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
            console.error(`No autocomplete defined for ${entity}`)
        }

        silkStore
            .request({topic: channel, data: {...apiDetails, term: input, ruleId}})
            .map((returned) => {
                return {options: returned.body};
            })
            .multicast(replySubject).connect();
    });

    const editMappingRule = (payload, id, parent) => {

        if (id) {

            return silkStore
                .request({
                    topic: 'transform.task.rule.put', data: {
                        ...apiDetails, ruleId: id, payload,
                    }
                })

        } else {

            return silkStore
                .request({
                    topic: 'transform.task.rule.rules.append', data: {
                        ...apiDetails, ruleId: parent, payload,
                    }
                })
        }
    };

    hierarchicalMappingChannel.subject('rule.createValueMapping').subscribe(({data, replySubject}) => {
            const payload = prepareValueMappingPayload(data);
            const parent = data.parentId ? data.parentId : rootId;

            editMappingRule(payload, data.id, parent)
                .multicast(replySubject).connect();


        }
    );

    hierarchicalMappingChannel.subject('rule.createObjectMapping').subscribe(({data, replySubject}) => {

            const payload = prepareObjectMappingPayload(data);
            const parent = data.parentId ? data.parentId : rootId;

            editMappingRule(payload, data.id, parent)
                .multicast(replySubject).connect();

        }
    );

    hierarchicalMappingChannel.subject('rule.createGeneratedMapping').subscribe(({data, replySubject}) => {

            const payload = data;
            const parent = data.parentId ? data.parentId : rootId;

            editMappingRule(payload, data.id, parent)
                .multicast(replySubject).connect();

        }
    );

    hierarchicalMappingChannel.subject('rule.removeRule').subscribe(
        ({data, replySubject}) => {
            const {id} = data;
            silkStore
                .request({
                    topic: 'transform.task.rule.delete', data: {
                        ...apiDetails, ruleId: id,
                    }
                })
                .subscribe(
                    () => {
                        replySubject.onNext();
                        replySubject.onCompleted();
                        hierarchicalMappingChannel.subject('reload').onNext(true);
                    },
                    (err) => {
                        //TODO: Beautify
                        console.warn(`Error deleting rule in ${id}`, err);
                        alert(`Error creating rule in ${id}`);
                    }
                )
        }
    );

    //TODO: implement
    hierarchicalMappingChannel.subject('rule.orderRule').subscribe(
        ({data, replySubject}) => {
            console.warn('TODO: implement')
        }
    );


} else {

    const rawMockStore = require('./retrieval2.json');

    let mockStore = null;

    try {
        mockStore = JSON.parse(localStorage.getItem('mockStore'));
    } catch (e) {
    }

    if (mockStore === null) {
        mockStore = _.cloneDeep(rawMockStore);
    }

    hierarchicalMappingChannel.subject('rules.generate').subscribe(
        ({data, replySubject}) => {
            const {correspondences, parentRuleId} = data;

            let rules = [];

            _.map(correspondences, (correspondence) => {
                rules.push(
                    {
                        "metadata": {
                            "description": _.includes(correspondence.sourcePath, 'error') ? 'error' : ''
                        },
                        "mappingTarget": {
                            "uri": correspondence.targetProperty,
                            "valueType": {
                                "nodeType": "AutoDetectValueType"
                            }
                        },
                        "sourcePath": correspondence.sourcePath,
                        "type": "direct"
                    }
                );
            });

            replySubject.onNext({rules});
            replySubject.onCompleted();
        }
    );

    hierarchicalMappingChannel.subject('rule.suggestions').subscribe(
        ({data, replySubject}) => {
            const paths = ['/name', '/city','/loan','/country','/lastname','/firstName','/address', '/expected-error'];
            const types = ['/name', '/city','/loan','/country','/lastname','/firstName','/address', '/one-error'];
            let suggestions = {};
            _.forEach(data.targetClassUris, (target) => {
                _.forEach(types, (type, key) => {
                    const path = paths[key];
                    suggestions[`${target}${type}`] = [{
                        "uri": path,
                        "confidence": Math.floor(100 - 0.1 * Math.random() * 100) / 100
                    }];
                })
            });

            replySubject.onNext({suggestions});
            replySubject.onCompleted();
        }
    );

    hierarchicalMappingChannel.subject('autocomplete').subscribe(({data, replySubject}) => {

        const {entity, input} = data;

        let result = [];

        switch (entity) {
        case 'propertyType':
            filterPropertyType(input, replySubject);
            return;
        case 'targetProperty':
            result = [
                {
                    value: 'http://xmlns.com/foaf/0.1/knows', label: 'foaf:knows',
                    description: 'A person known by this person (indicating some level of reciprocated interaction between the parties).'
                }, {
                    value: 'http://xmlns.com/foaf/0.1/name', label: 'foaf:name',
                    description: 'A name for some thing.'
                }, {
                    value: 'http://schmea.org/address', label: 'schema:address',
                    description: 'Physical address of the item.'
                }
            ];
            break;
        case 'targetEntityType':
            result = [
                {
                    value: 'http://xmlns.com/foaf/0.1/Person', label: 'foaf:Person',
                    description: 'The Person class represents people. Something is a Person if it is a person. We don\'t nitpic about whether they\'re alive, dead, real, or imaginary. The Person class is a sub-class of the Agent class, since all people are considered \'agents\' in FOAF.'

                }, {
                    value: 'http://schema.org/PostalAddress', label: 'schema:PostalAddress',
                    description: 'The mailing address.'
                }
            ];
            break;
        case 'sourcePath':
            result = [
                {value: '/name', label: 'name',},
                {value: '/address', label: 'address',},
                {value: '/last_name', label: 'last name',},
            ];
            break;
        default:
            console.error(`No autocomplete defined for ${entity}`)
        }

        const search = _.isString(input) ? input.toLocaleLowerCase() : '';

        replySubject.onNext({
            options: _.filter(result, ({value, label, description}) => {
                return _.includes(value.toLocaleLowerCase(), search)
                    || _.includes(label.toLocaleLowerCase(), search)
                    || _.includes(description.toLocaleLowerCase(), search)
            })
        });

        replySubject.onCompleted();

    });

    hierarchicalMappingChannel.subject('hierarchy.get').subscribe(
        ({data, replySubject}) => {
            const hierarchy = _.chain(mockStore)
            //TODO: Filter only hierarchical mappings
                .value();

            // `replySubject` is just a Rx.AsyncSubject
            replySubject.onNext({hierarchy});
            replySubject.onCompleted();
        }
    );

    hierarchicalMappingChannel.subject('rule.example').subscribe(
        ({data, replySubject}) => {
            //const {id} = data;
            const example = {
                "sourcePaths": [["/name"], ["/birthdate"]],
                "results": [{
                    "sourceValues": [["Abigale Purdy"], ["7/21/1977"]],
                    "transformedValues": ["abigale purdy7/21/1977"]
                }, {
                    "sourceValues": [["Ronny Wiegand"], ["10/24/1963"]],
                    "transformedValues": ["ronny wiegand10/24/1963"]
                }, {
                    "sourceValues": [["Rosalyn Wisozk"], ["5/8/1982"]],
                    "transformedValues": ["rosalyn wisozk5/8/1982"]
                }],
                "status": {"id": "success", "msg": ""}
            };
            replySubject.onNext({example});
            replySubject.onCompleted();
        }
    );


    hierarchicalMappingChannel.subject('rule.get').subscribe(
        ({data, replySubject}) => {

            const {id} = data;
            const searchId = id ? id : mockStore.id;
            const rule = findRule(_.cloneDeep(mockStore), searchId, []);
            const result = _.isUndefined(rule) ? mockStore : rule;
            replySubject.onNext({rule: result});
            replySubject.onCompleted();

        }
    );

    const appendToMockStore = (mockStore, id, payload) => {
        if (mockStore.id === id && _.has(mockStore, 'rules.propertyRules')) {
            mockStore.rules.propertyRules.push(payload);
        }
        else if (mockStore.id === id) {
            mockStore.rules.propertyRules = [payload];
        } else if (_.has(mockStore, 'rules.propertyRules')) {
            _.forEach(_.get(mockStore, 'rules.propertyRules'), (childRule) => {
                appendToMockStore(childRule, id, payload);
            });
        }

    };

    const editRule = (mockStore, id, payload) => {
        if (mockStore.id === id) {
            if (_.has(mockStore.rules, 'typeRules') && _.has(payload.rules, 'typeRules')){
                mockStore.rules.typeRules = payload.rules.typeRules;
            }
            _.merge(mockStore, payload)
        } else if (_.has(mockStore, 'rules.propertyRules')) {
            _.forEach(_.get(mockStore, 'rules.propertyRules'), (childRule) => {
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
                issues: [{message: 'None really, we just want to test the feature'}]
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

        const payload = _.includes(['object', 'root'], data.type) ? prepareObjectMappingPayload(data) : prepareValueMappingPayload(data);

        if (_.includes(data.comment, 'error')) {
            const err = new Error('Could not save rule.');
            _.set(err, 'response.body', {
                message: 'Comment cannot contain "error"',
                issues: [{message: 'None really, we just want to test the feature'}]
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

    hierarchicalMappingChannel.subject('rule.createValueMapping').subscribe(handleUpdate);

    hierarchicalMappingChannel.subject('rule.createObjectMapping').subscribe(handleUpdate);

    hierarchicalMappingChannel.subject('rule.createGeneratedMapping').subscribe(handleUpdatePreparedRule);
    const removeRule = (store, id) => {

        if (store.id === id) {
            return null;
        } else if (_.has(store, 'rules.propertyRules')) {
            store.rules.propertyRules = _.filter(store.rules.propertyRules, (v) => removeRule(v, id) !== null);
        }
        return store;
    };

    Array.prototype.move = function(old_index, new_index) {
        if (new_index >= this.length) {
            var k = new_index - this.length;
            while ((k--) + 1) {
                this.push(undefined);
            }
        }
        this.splice(new_index, 0, this.splice(old_index, 1)[0]);
        return this; // for testing purposes
    };

    hierarchicalMappingChannel.subject('rule.removeRule').subscribe(
        ({data, replySubject}) => {
            const {id} = data;
            mockStore = removeRule(_.chain(mockStore).value(), id);
            saveMockStore();
            replySubject.onNext();
            replySubject.onCompleted();
        }
    );

    const orderRule = (store, id, pos) => {
        if (_.has(store, 'rules.propertyRules')) {
            const idPos = _.reduce(store.rules.propertyRules, function(i, children, k) {
                if (i > -1 || children.id !== id)
                    return i;
                else
                    return k;
            }, -1);
            if (idPos > -1) {
                pos = pos < 0 ? pos + store.rules.propertyRules.length : pos;
                store.rules.propertyRules.move(idPos, pos)

            } else {
                store.rules.propertyRules = _.map(store.rules.propertyRules, (v) => orderRule(v, id, pos));
            }
        }
        return store;

    };

    hierarchicalMappingChannel.subject('rule.orderRule').subscribe(
        ({data, replySubject}) => {
            const {pos, id} = data;
            mockStore = orderRule(_.chain(mockStore).value(), id, pos);
            saveMockStore();
            replySubject.onNext();
            replySubject.onCompleted();
        }
    );

    const loremIpsum = require('lorem-ipsum');


    hierarchicalMappingChannel.subject('vocabularyInfo.get').subscribe(
        ({data, replySubject}) => {

            const {uri, field} = data;

            const ret = {info: null};

            switch (field) {
            case 'label':
                break;
            case 'description':
                ret.info = loremIpsum({
                    count: _.random(0, 2),
                    units: 'paragraphs'
                });
            }

            replySubject.onNext(ret);
            replySubject.onCompleted();


        }
    );


}

export default hierarchicalMappingChannel;
