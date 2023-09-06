import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";

export function getItemLinkIcons(label: string): ValidIconName | undefined {
    switch (label) {
        case "Mapping editor":
        case "Linking editor":
            return "application-mapping";
        case "Transform evaluation":
        case "Linking evaluation":
            return "item-evaluation";
        case "Transform execution":
        case "Linking execution":
            return "item-execution";
        default:
    }
}
