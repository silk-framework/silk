// Store specific to hierarchical mappings, will use silk-store internally

import rxmq from 'ecc-messagebus';
import _ from 'lodash';


const rawMockStore = require('./retrieval2.json');

let mockStore = null;

try {
    mockStore = JSON.parse(localStorage.getItem('mockStore'));
} catch (e) {
}

if(mockStore === null){
    mockStore = _.cloneDeep(rawMockStore);
}


const hierarchicalMappingChannel = rxmq.channel('silk.hierarchicalMapping');

// Set api details
let transformationTask = false;
let apiBase = false;
let project = false;

// Set Api details
hierarchicalMappingChannel.subject('setSilkDetails').subscribe();

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

const findRule = (parentRule, id) => {

    let foundRule = null;

    if (parentRule.id === id) {
        return parentRule;
    } else if (_.has(parentRule, 'rules.propertyRules')) {
        _.forEach(_.get(parentRule, 'rules.propertyRules'), (childRule) => {
            if (childRule.id === id) {
                if (childRule.type === 'object') {
                    foundRule = childRule;
                } else {
                    foundRule = parentRule;
                }
                return false;
            }
            if (_.has(childRule, 'rules.propertyRules')) {
                foundRule = findRule(childRule, id);
                if (foundRule) {
                    return false;
                }
            }
        });
        return foundRule;
    }

};

hierarchicalMappingChannel.subject('rule.get').subscribe(
    ({data, replySubject}) => {

        const {id} = data;

        const rule = id ? findRule(mockStore, id) : mockStore;

        replySubject.onNext({rule: rule ? rule : mockStore});
        replySubject.onCompleted();
    }
);

const appendToMockStore = (mockStore, id, payload) => {
    if (mockStore.id === id && _.has(mockStore, 'rules.propertyRules')) {
        mockStore.rules.propertyRules.push(payload);
    } else if (_.has(mockStore, 'rules.propertyRules')) {
        _.forEach(_.get(mockStore, 'rules.propertyRules'), (childRule) => {
            appendToMockStore(childRule, id, payload);
        });
    }

};

const editRule = (mockStore, id, payload) => {
    if (mockStore.id === id) {
        _.merge(mockStore, payload)
    } else if (_.has(mockStore, 'rules.propertyRules')) {
        _.forEach(_.get(mockStore, 'rules.propertyRules'), (childRule) => {
            editRule(childRule, id, payload);
        });
    }

};

const saveMockStore = () => {
    hierarchicalMappingChannel.subject('reload').onNext(true);
    localStorage.setItem('mockStore', JSON.stringify(mockStore))
};

hierarchicalMappingChannel.subject('rule.createValueMapping').subscribe(
    (data) => {

        console.warn(data);

        const payload = {
            comment: data.comment,
            "mappingTarget": {
                "uri": data.targetProperty,
                "valueType": {
                    "nodeType": data.propertyType,
                }
            }
        };

        if (data.type === 'direct') {
            payload.sourcePath = data.sourceProperty;
        }

        if (data.id) {

            editRule(mockStore, data.id, payload);

        } else {

            payload.id = _.uniqueId('valueMapping');
            payload.type = data.type;

            const parent = data.parentId ? data.parentId : mockStore.id;

            appendToMockStore(mockStore, parent, payload);
        }

        saveMockStore();

    }
);

hierarchicalMappingChannel.subject('rule.createObjectMapping').subscribe(
    (data) => {

        // TODO: What the heck is sourcePath here and where to save from/to
        const payload = {
            "mappingTarget": {
                "uri": data.targetProperty,
                "valueType": {
                    "nodeType": "UriValueType",
                }
            },
            "rules": {
                "uriRule": data.pattern ? {
                    "type": "uri",
                    "id": _.uniqueId('uriRulePattern'),
                    "pattern": data.pattern
                } : null,
                "typeRules": [
                    {
                        "type": "type",
                        "id": _.uniqueId('typeRuleID'),
                        "typeUri": data.targetEntityType,
                    }
                ],
            }
        };

        if (data.id) {

            editRule(mockStore, data.id, payload);

        } else {

            payload.id = _.uniqueId('objectMapping');
            payload.type = 'object';

            const parent = data.parentId ? data.parentId : mockStore.id;

            appendToMockStore(mockStore, parent, payload);
        }

        saveMockStore();


    }
);

export default hierarchicalMappingChannel;
