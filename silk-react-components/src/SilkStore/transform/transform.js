// Low level representation of silks /transform API

import rxmq from 'ecc-messagebus';
import superagent from 'ecc-superagent';

const silkStore = rxmq.channel('silk.api');


//TODO: Implement those once needed
silkStore.subject('transform.task.get').subscribe();
silkStore.subject('transform.task.put').subscribe();
silkStore.subject('transform.task.delete').subscribe();

silkStore.subject('transform.task.rules.get')
    .subscribe(({data, replySubject}) => {

            const {baseUrl, project, transformTask} = data;

            superagent
                .get(`${baseUrl}/transform/tasks/${project}/${transformTask}/rules`)
                .accept('application/json')
                .observe()
                .multicast(replySubject).connect();

        }
    );
//TODO: Implement once needed
silkStore.subject('transform.task.rules.put').subscribe();

//TODO: Implement once needed
silkStore.subject('transform.task.rule.get').subscribe();
silkStore.subject('transform.task.rule.put').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, ruleId, payload} = data;

        superagent
            .put(`${baseUrl}/transform/tasks/${project}/${transformTask}/rule/${ruleId}`)
            .accept('application/json')
            .type('application/json')
            .send(payload)
            .observe()
            .multicast(replySubject).connect();

    }
);

silkStore.subject('transform.task.rule.delete').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, ruleId} = data;

        superagent
            .del(`${baseUrl}/transform/tasks/${project}/${transformTask}/rule/${ruleId}`)
            .accept('application/json')
            .observe()
            .multicast(replySubject).connect();

    }
);

silkStore.subject('transform.task.rule.peak').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, id} = data;

        superagent
            .post(`${baseUrl}/transform/tasks/${project}/${transformTask}/peak/${id}`)
            .accept('application/json')
            .observe()
            .multicast(replySubject).connect();

    }
);

silkStore.subject('transform.task.rule.rules.append').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, ruleId, payload} = data;

        superagent
            .post(`${baseUrl}/transform/tasks/${project}/${transformTask}/rule/${ruleId}/rules`)
            .accept('application/json')
            .type('application/json')
            .send(payload)
            .observe()
            .multicast(replySubject).connect();

    }
);

//TODO: Implement once needed
silkStore.subject('transform.task.rule.rules.reorder').subscribe();

silkStore.subject('transform.task.rule.completions.sourcePaths').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, ruleId, term, maxResults = 30} = data;

        superagent
            .get(`${baseUrl}/transform/tasks/${project}/${transformTask}/rule/${ruleId}/completions/sourcePaths`)
            .query({
                term,
                maxResults,
            })
            .accept('application/json')
            .type('application/json')
            .observe()
            .multicast(replySubject).connect();

    }
);
silkStore.subject('transform.task.rule.completions.targetProperties').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, ruleId, term, maxResults = 30} = data;

        superagent
            .get(`${baseUrl}/transform/tasks/${project}/${transformTask}/rule/${ruleId}/completions/targetProperties`)
            .query({
                term,
                maxResults,
            })
            .accept('application/json')
            .type('application/json')
            .observe()
            .multicast(replySubject).connect();

    }
);
silkStore.subject('transform.task.rule.completions.targetTypes').subscribe(({data, replySubject}) => {

        const {baseUrl, project, transformTask, ruleId, term, maxResults = 30} = data;

        superagent
            .get(`${baseUrl}/transform/tasks/${project}/${transformTask}/rule/${ruleId}/completions/targetTypes`)
            .query({
                term,
                maxResults,
            })
            .accept('application/json')
            .type('application/json')
            .observe()
            .multicast(replySubject).connect();

    }
);
