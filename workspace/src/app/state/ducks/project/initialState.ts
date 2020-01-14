import { IProjectState } from "./typings";

export function initialProjectState(): IProjectState {
    return {
        id: '',
        isLoading: false,
        error: {},
        data: {}
    }
}
