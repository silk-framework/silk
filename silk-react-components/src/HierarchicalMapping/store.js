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

const findRule = (parentRule, id, breadcrumbs) => {

    let foundRule = null;

    if (parentRule.id === id) {
        parentRule.breadcrumbs = breadcrumbs;
        return parentRule;
    } else if (_.has(parentRule, 'rules.propertyRules')) {

        const bc = [...breadcrumbs, {
            id: parentRule.id,
            name: parentRule.type === 'root' ?
                _.get(parentRule, 'rules.typeRules[0].typeUri', '(no target type)') :
                _.get(parentRule, 'mappingTarget.uri', '(no target property)')
        }];

        _.forEach(_.get(parentRule, 'rules.propertyRules'), (childRule) => {
            if (childRule.id === id) {
                if (childRule.type === 'object') {
                    foundRule = childRule;
                } else {
                    foundRule = parentRule;
                }
                foundRule.breadcrumbs = bc;
                return false;
            }
            if (_.has(childRule, 'rules.propertyRules')) {
                foundRule = findRule(childRule, id, bc);
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

        const searchId = id ? id : mockStore.id;

        const rule = findRule(_.cloneDeep(mockStore), searchId, []);

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
    localStorage.setItem('mockStore', JSON.stringify(mockStore));
};

hierarchicalMappingChannel.subject('rule.createValueMapping').subscribe(
    (data) => {

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

        // TODO: What the heck is sourcePath here? We do not set it in the UI
        const payload = {
            comment: data.comment,
            "mappingTarget": {
                "uri": data.targetProperty,
                "inverse": data.entityConnection,
                "valueType": {
                    "nodeType": "UriValueType",
                }
            },
            "rules": {
                "uriRule": data.pattern ? {
                    "type": "uri",
                    "pattern": data.pattern
                } : null,
                "typeRules": [
                    {
                        "type": "type",
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

const removeRule = (store, id) => {

    if (store.id===id) {
        return null;
    } else if (_.has(store, 'rules.propertyRules')) {
        store.rules.propertyRules = _.filter(store.rules.propertyRules, (v) => removeRule(v, id) !== null);
    }
    return store;
};

Array.prototype.move = function (old_index, new_index) {
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
    console.log(store.parent, store.id)
    if (_.has(store, 'rules.propertyRules')) {
        const idPos = _.reduce(store.rules.propertyRules, function(i, children, k) {
            if (i > -1 && children.id !== id)
                return i;
            else
                return k;
            }, -1);
        if (idPos > -1){
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

export default hierarchicalMappingChannel;
