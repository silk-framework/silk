export interface IModifierOption {
    id: string;
    label: string;
}

export interface IModifierState {
    label: string;
    field: string;
    options: IModifierOption[];
}

export interface IModifiersState {
    [key: string]: IModifierState
}

export function initialModifierState(props: Partial<IModifierState> = {}): IModifierState {
    return {
        label: '',
        field: '',
        options: [],
        ...props
    }
}
