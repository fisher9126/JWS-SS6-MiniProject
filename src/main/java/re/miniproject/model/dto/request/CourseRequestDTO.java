package re.miniproject.model.dto.request;



import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseRequestDTO {

    @NotBlank
    private String name;

    private String description;

    @Min(0)
    private Double price;


}

