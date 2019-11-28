import { FiltersDto } from "./filters/dtos";
import { PreviewDto } from "./preview/dtos";

export class DashboardDto {
    filters: FiltersDto = new FiltersDto();
    preview: PreviewDto = new PreviewDto();
}
