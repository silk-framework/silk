export class Suggestion {
    constructor(path, type = 'value', target = null, confidence = null) {
        this.sourcePath = path;
        this.targetProperty = target;
        this.confidence = confidence;
        this.id = `${this.sourcePath}${this.targetProperty}`;
        this.order = this.confidence || 0;
        this.type = type;
    }
}
