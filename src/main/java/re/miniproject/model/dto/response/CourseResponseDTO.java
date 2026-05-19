package re.miniproject.model.dto.response;

import lombok.Data;

@Data
public class CourseResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private String imageUrl;


}

