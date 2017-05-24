// Store specific to hierarchical mappings, will use silk-store internally

import rxmq from 'ecc-messagebus';
import _ from 'lodash';
const mockStore = require('./retrieval2.json');

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

hierarchicalMappingChannel.subject('rule.get').subscribe(
    ({data, replySubject}) => {

        const {id} = data;

        const rule = _.chain(mockStore)
        //TODO: Search the hierarchical Mapping with the matching id
            .value();

        replySubject.onNext({rule});
        replySubject.onCompleted();
    }
);


export default hierarchicalMappingChannel;
