import { workspacePath } from "../../TestHelper";
import { rangeArray } from "../../../../src/app/utils/basicUtils";

export class RelatedItemsTestHelper {
    /** Generates a JSON for response for the related items endpoint. */
    static generateRelatedItemsJson(nrItems: number, itemIdPrefix: string): Object {
        return {
            items: rangeArray(nrItems).map((itemIdx) => {
                const itemId = `${itemIdPrefix}${itemIdx}`;
                return {
                    description: `${itemId} description`,
                    id: itemId,
                    itemLinks: [
                        {
                            label: "Item details page",
                            path: workspacePath(`/projects/cmem/task/${itemId}`),
                        },
                        {
                            label: "Item editor",
                            path: `/editor/cmem/${itemId}/editor`,
                        },
                        {
                            label: "Item editor 2",
                            path: `/editor/cmem/${itemId}/editor`,
                        },
                    ],
                    label: `${itemId} label`,
                    type: "Dataset",
                    pluginLabel: "testPlugin",
                };
            }),
            total: nrItems,
        };
    }
}
