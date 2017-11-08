export class Suggestion {
    constructor(path, target = null, confidence = null) {
        this.sourcePath = path;
        this.targetProperty = target;
        this.confidence = confidence;
        this.id = `${this.sourcePath}${this.targetProperty}`;
        this.order = this.confidence || 0;
    }
}
