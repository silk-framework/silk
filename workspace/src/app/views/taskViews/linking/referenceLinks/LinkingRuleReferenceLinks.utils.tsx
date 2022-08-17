import { IEntity, IEntitySchema } from "../../shared/rules/rule.typings";
import { EntityLink } from "./LinkingRuleReferenceLinks.typing";
import { IEntityLink } from "../linking.types";

/** Chooses a specific property value based on the path index or optionally based on the given entity schema. */
const pickPropertyValue = (path: string, pathIdx: number, values: string[][], schema: IEntitySchema): string[] => {
    const valueIdx = pathIdx;
    if (schema) {
        const pathIdx = schema.paths.findIndex((p) => p === path);
        if (pathIdx < 0 || pathIdx >= values.length) {
            return [];
        } else {
            return values[pathIdx];
        }
    }
    return values[valueIdx] ?? [];
};

/** Choose reference links path values based on the requested properties. */
const pickEntityValues = (entity: IEntity, paths?: string[]): string[][] => {
    const entityValues = entity.values;
    const entitySchema = entity.schema;
    if (paths && entitySchema) {
        return paths.map((path, idx) => {
            return pickPropertyValue(path, idx, entityValues, entitySchema);
        });
    } else {
        // No specific properties requested, return values of first property
        return (paths ?? []).map((p) => []);
    }
};

/** Same as above, but all values are concatenated to a single string. */
const entityValuesConcatenated = (entity: IEntity, paths?: string[], separator: string = ", "): string => {
    return pickEntityValues(entity, paths)
        .filter((vs) => vs.length > 0)
        .map((vs) => vs.join(separator))
        .join(separator);
};

/** Convert the entity link format from the backend. */
const toReferenceEntityLink = (entityLink: IEntityLink): EntityLink | undefined => {
    return entityLink.entities
        ? {
              source: entityLink.entities.source,
              target: entityLink.entities.target,
              entityLinkId: `${entityLink.source} ${entityLink.target}`,
              decision: entityLink.decision ?? "unlabeled",
          }
        : undefined;
};

const utils = {
    pickPropertyValue,
    pickEntityValues,
    toReferenceEntityLink,
    entityValuesConcatenated,
};

export default utils;
