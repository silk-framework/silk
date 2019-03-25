import nock from "nock";

// default baseUrl in case nothing else is defined.
// for more details see store.js:
// hierarchicalMappingChannel.subject('setSilkDetails')
const baseUrl = ' http://test.url:80';

const mockUpFunction = (identifier) => {

	const dataURL = (testCase) => {
		console.log(testCase)
		return {
			url: `/transform/tasks/test/test/peak/${testCase}`
		};
	};

	const getResponse = (testCase) => {
		switch (testCase) {
			case "country":
				return {
					code: 200,
					body: {
						"sourcePaths": [["/dbpediaowl:country"]],
						"results": [{
							"sourceValues": [["http://dbpedia.org/resource/United_States"]],
							"transformedValues": ["http://dbpedia.org/resource/United_States"]
						}, {
							"sourceValues": [["http://dbpedia.org/resource/Cinema_of_Denmark", "http://dbpedia.org/resource/Cinema_of_Sweden", "http://dbpedia.org/resource/Cinema_of_the_United_Kingdom"]],
							"transformedValues": ["http://dbpedia.org/resource/Cinema_of_Denmark", "http://dbpedia.org/resource/Cinema_of_Sweden", "http://dbpedia.org/resource/Cinema_of_the_United_Kingdom"]
						}, {
							"sourceValues": [["http://dbpedia.org/resource/United_States"]],
							"transformedValues": ["http://dbpedia.org/resource/United_States"]
						}],
						"status": { "id": "success", "msg": "" }
					}
				};
			case "basedOn":
				return {
					code: 200,
					body: {
						"sourcePaths": [["/dbpediaowl:basedOn"]],
						"results": [{
							"sourceValues": [["http://dbpedia.org/resource/Dawn_of_the_Dead"]],
							"transformedValues": ["http://dbpedia.org/resource/Dawn_of_the_Dead"]
						}, {
							"sourceValues": [["http://dbpedia.org/resource/Philip_K._Dick", "http://dbpedia.org/resource/The_Minority_Report"]],
							"transformedValues": ["http://dbpedia.org/resource/Philip_K._Dick", "http://dbpedia.org/resource/The_Minority_Report"]
						}, {
							"sourceValues": [["http://dbpedia.org/resource/The_Count_of_Monte_Cristo"]],
							"transformedValues": ["http://dbpedia.org/resource/The_Count_of_Monte_Cristo"]
						}],
						"status": { "id": "success", "msg": "" }
					}
				};
			default:
				throw new Error(`Undefined response for code ${testCase}`);
		}
	};

	const payload = dataURL(identifier);
	const response = getResponse(identifier);

	nock(baseUrl)
		.log(console.log)
		.post(payload.url)
		.reply(response.code, () => response.body);
};

// mock up testCases cases

// 3 and 2 suggestions
mockUpFunction("basedOn");

mockUpFunction('country');
