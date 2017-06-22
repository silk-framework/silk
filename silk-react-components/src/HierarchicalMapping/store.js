// Store specific to hierarchical mappings, will use silk-store internally

import _ from 'lodash';
import rxmq from 'ecc-messagebus';
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
                name: element.type === 'root'
                    ? _.get(element, 'rules.typeRules[0].typeUri', '(no target type)')
                    : _.get(element, 'mappingTarget.uri', '(no target property)')
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
                "nodeType": data.propertyType,
            }
        }
    };

    if (data.type === 'direct') {
        payload.sourcePath = data.sourceProperty || '';
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
        sourcePath: data.sourceProperty || '',
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

    hierarchicalMappingChannel.subject('rule.example').subscribe(
        ({data, replySubject}) => {

            const {id} = data;
            if (id) {
                silkStore
                    .request({topic: 'transform.task.rule.peak', data: {...apiDetails, id}}).
                map((returned) => {
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
                        console.warn(`Error saving rule in ${id}`, err);
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
            ///transform/tasks/{project}/{transformationTask}/peak/{rule}
            //const {id} = data;
            const example = {"sourcePaths":[["/name"],["/birthdate"]],"results":[{"sourceValues":[["Abigale Purdy"],["7/21/1977"]],"transformedValues":["abigale purdy7/21/1977"]},{"sourceValues":[["Ronny Wiegand"],["10/24/1963"]],"transformedValues":["ronny wiegand10/24/1963"]},{"sourceValues":[["Rosalyn Wisozk"],["5/8/1982"]],"transformedValues":["rosalyn wisozk5/8/1982"]}],"status":{"id":"success","msg":""}}
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

    const handleUpdate = ({data, replySubject}) => {


        const payload = _.includes(['object', 'root'], data.type) ? prepareObjectMappingPayload(data) : prepareValueMappingPayload(data);

        console.warn('MOCKSTORE: Saving: ', JSON.stringify(payload, null, 2));

        if (_.includes(data.comment, 'error')) {
            const err = new Error('Could not save rule.');
            _.set(err, 'response.body', {
                message: 'Comment cannot contain "error"',
                issues: [{message: 'None really, we just want to test the feature'}]
            });
            replySubject.onError(err);
            replySubject.onCompleted();

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

}

export default hierarchicalMappingChannel;
