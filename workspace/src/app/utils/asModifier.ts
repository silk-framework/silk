export default function asModifier(label: string, field: string, options: any[]) {
    return {
        label,
        field: field || label,
        options
    }
}
