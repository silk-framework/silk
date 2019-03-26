import nock from "nock";

// default baseUrl in case nothing else is defined.
// for more details see store.js:
// hierarchicalMappingChannel.subject('setSilkDetails')
const baseUrl = ' http://test.url:80';

/**
 * mocking up the backend functionality
 *
 * @param identifier {string}
 */
const mockUpFunction = (identifier) => {
	/**
	 * getting the URL for data
	 *
	 * @param testCase
	 * @returns {{url: string}}
	 */
	const dataURL = (testCase) => {
		return {
			url: `/transform/tasks/test/test/peak/${testCase}`
		};
	};

	/**
	 * Getting the response form URL
	 *
	 * @param testCase
	 * @returns {*}
	 */
	const getResponse = (testCase) => {
		return {
			code: 200,
			body: {
				sourcePaths: [
					[`/dbpediaowl:${testCase}`]
				],
				results: [
					{
						sourceValues: [
							["http://dbpedia.org/resource/United_States"]
						],
						transformedValues: ["http://dbpedia.org/resource/United_States"]
					},
					{
						sourceValues: [
							["http://dbpedia.org/resource/Cinema_of_Denmark", "http://dbpedia.org/resource/Cinema_of_Sweden", "http://dbpedia.org/resource/Cinema_of_the_United_Kingdom"]
						],
						transformedValues: ["http://dbpedia.org/resource/Cinema_of_Denmark", "http://dbpedia.org/resource/Cinema_of_Sweden", "http://dbpedia.org/resource/Cinema_of_the_United_Kingdom"]
					},
					{
						sourceValues: [
							["http://dbpedia.org/resource/United_States"]
						],
						transformedValues: ["http://dbpedia.org/resource/United_States"]
					}
				],
				"status": { "id": "success", "msg": "" }
			}
		};

	};

	/**
	 * Getting URl of rules
	 *
	 * @returns {{url: string}}
	 */
	const rulesDataURL = () => {
		return {
			url: '/transform/tasks/test/test/rules'
		};
	};

	/**
	 * Getting response for rules
	 *
	 * @returns {{code: number, body: {metadata: {label: string}, rules: {typeRules: Array, propertyRules: *[], uriRule: null}, id: string, type: string}}}
	 */
	const getRulesResponse = () => {
		return {
			code: 200,
			body: {
				type: "root",
				id: "root",
				rules: {
					uriRule: null,
					typeRules: [],
					propertyRules: [
						{
							type: "direct",
							id: "country",
							sourcePath: "dbpediaowl:country",
							mappingTarget: {
								uri: "<urn:ruleProperty:country>",
								valueType: {
									nodeType: "StringValueType"
								},
								isBackwardProperty: false,
								isAttribute: false
							},
							metadata: {
								label: ""
							}
						},
						{
							type: "direct",
							id: "basedOn",
							sourcePath: "dbpediaowl:basedOn",
							mappingTarget: {
								uri: "<urn:ruleProperty:basedOn>",
								valueType: { "nodeType": "StringValueType" },
								isBackwardProperty: false,
								isAttribute: false
							},
							metadata: {
								label: ""
							}
						}
					]
				},
				metadata: {
					label: "Root Mapping"
				}
			}
		};
	};

	/**
	 * Getting the URL of copyRule
	 *
	 * @param sourceRule {string}
	 * @returns {{data: {sourceTask: string, sourceRule: *, afterRuleId: null, sourceProject: string}, url: string}}
	 */
	const rulesDataURLCopyFrom = (sourceRule) => {
		return {
			url: `/transform/tasks/movies/test/rule/root/rules/copyFrom`,
			data: {
				sourceProject: 'test',
				sourceTask: 'test',
				sourceRule: sourceRule,
				afterRuleId: null
			}
		};
	};

	/**
	 * Getting the response for copyRule
	 *
	 * @param testCase {string}
	 * @returns {{code: number, body: {metadata: {label: string}, id: string, type: string, sourcePath: string, mappingTarget: {isAttribute: boolean, valueType: {nodeType: string}, uri: string, isBackwardProperty: boolean}}}}
	 */
	const getRulesDataURLCopyFromResponse = (testCase) => {
		return {
			code: 200,
			body: {
				type: "direct",
				id: `${testCase}1`,
				sourcePath: `dbpediaowl:${testCase}`,
				mappingTarget: {
					uri: `<urn:ruleProperty:${testCase}>`,
					valueType: {
						nodeType: "StringValueType"
					},
					isBackwardProperty: false,
					isAttribute: false
				},
				metadata: {
					label: `Copy of urn:ruleProperty:${testCase}`
				}
			}
		};
	};

	/**
	 * @type {{url: string}}
	 */
	const payload = dataURL(identifier),

		/**
		 * @type {{code, body}}
		 */
		response = getResponse(identifier),

		/**
		 * @type {{url}}
		 */
		rulesPayload = rulesDataURL(),

		/**
		 * @type {{code, body}}
		 */
		rulesResponse = getRulesResponse(),

		/**
		 * @type {{data, url}}
		 */
		rulesDataURLCopyFromPayload = rulesDataURLCopyFrom(identifier),

		/**
		 * @type {{code, body}}
		 */
		rulesDataURLCopyFromResponse = getRulesDataURLCopyFromResponse(identifier);

	nock(baseUrl)
		.post(payload.url)
		.reply(response.code, () => response.body);

	nock(baseUrl)
		.get(rulesPayload.url)
		.reply(rulesResponse.code, rulesResponse.body);

	nock(baseUrl)
		.get(rulesDataURLCopyFromPayload.url, rulesDataURLCopyFromPayload.data)
		.reply(rulesDataURLCopyFromResponse.code, rulesDataURLCopyFromResponse.body);
};

// mock up testCases cases
mockUpFunction("basedOn");

mockUpFunction('country');
