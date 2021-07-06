export function getItemLinkIcons(label: string): string | undefined {
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
