/** Input source of a task. */
export interface IInputSource {
    /** ID of the input task. */
    inputId: string;
    /** Requested type of the input task. */
    typeUri?: string;
    /** Optional restriction for RDF/SPARQL based sources. Contains the body/pattern of a SPARQL WHERE clause. */
    restriction?: string;
}
