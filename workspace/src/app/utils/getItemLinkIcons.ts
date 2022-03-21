import { ValidIconName } from "gui-elements/src/components/Icon/canonicalIconNames";

export function getItemLinkIcons(label: string): ValidIconName | undefined {
    switch (label) {
        case "Mapping editor":
            return "application-mapping";
        case "Transform evaluation":
            return "item-evaluation";
        case "Transform execution":
            return "item-execution";
        default:
    }
}
