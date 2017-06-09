// Store specific to hierarchical mappings, will use silk-store internally

import rxmq from 'ecc-messagebus';
import _ from 'lodash';


const rawMockStore = require('./retrieval2.json');

let mockStore = null;

try {
    mockStore = JSON.parse(localStorage.getItem('mockStore'));
} catch (e) {
}

if (mockStore === null) {
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

function findRule(element, id, breadcrumbs){
    element.breadcrumbs = breadcrumbs;
    if(element.id === id){
        return element;
    }else if (_.has(element, 'rules.propertyRules')) {
        let result = null;
        const bc = [
            ...breadcrumbs,
            {
                id: element.id,
                name: element.type === 'root'
                    ? _.get(element, 'rules.typeRules[0].typeUri', '(no target type)')
                    : _.get(element, 'mappingTarget.uri', '(no target property)')
            }
        ];
        _.forEach(element.rules.propertyRules, (child) => {
            if (result !== null)
            result = findRule(child, id, bc);
        });

        return result;
    }
    return null;
}

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
        console.log('STORE', data)
        const payload = {
            "metadata": {
                description: data.comment,
            },
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
            hierarchicalMappingChannel.subject('ruleView.closed').onNext({id: payload.id});

        } else {

            payload.id = Date.now() + "" + _.random(0, 100, false);
            payload.type = data.type;

            const parent = data.parentId ? data.parentId : mockStore.id;
            appendToMockStore(mockStore, parent, payload);

            hierarchicalMappingChannel.subject('ruleView.created').onNext({id: payload.id});
        }

        saveMockStore();
    }
);

hierarchicalMappingChannel.subject('rule.createObjectMapping').subscribe(
    (data) => {
        console.log('STORE', data)
        const payload = {
            "metadata": {
                description: data.comment,
            },
            "mappingTarget": {
                "uri": data.targetProperty,
                "inverse": data.entityConnection,
                "valueType": {
                    "nodeType": "UriValueType",
                }
            },
            sourcePath: data.sourcePath,
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

            payload.id = Date.now() + "" + _.random(0, 100, false);
            payload.type = 'object';

            const parent = data.parentId ? data.parentId : mockStore.id;

            appendToMockStore(mockStore, parent, payload);
        }

        saveMockStore();
    }
);

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
            if (i > -1 && children.id !== id)
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

export default hierarchicalMappingChannel;
