package re.miniproject.service;



import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import re.miniproject.model.dto.request.CourseRequestDTO;
import re.miniproject.model.dto.response.CourseResponseDTO;
import re.miniproject.model.entity.Course;
import re.miniproject.repository.ICourseRepository;


@Service
@RequiredArgsConstructor
public class CourseService {

    private final ICourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;

    private CourseResponseDTO toDTO(Course course) {
        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setDescription(course.getDescription());
        dto.setPrice(course.getPrice());
        dto.setImageUrl(course.getImageUrl());
        return dto;
    }

    private void updateEntityFromDTO(Course course, CourseRequestDTO dto) {
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setPrice(dto.getPrice());
    }

    // ====== FR-01: GET ALL (PAGEABLE) ======

    public Page<CourseResponseDTO> getAllCourses(Pageable pageable) {
        Page<Course> page = courseRepository.findAll(pageable);
        return page.map(this::toDTO);
    }

    // ====== FR-02: GET BY ID ======

    public CourseResponseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toDTO(course);
    }

    // ====== FR-03: CREATE ======

    public CourseResponseDTO createCourse(CourseRequestDTO dto) {
        Course course = new Course();
        updateEntityFromDTO(course, dto);
        // imageUrl để null, không cho set từ client
        Course saved = courseRepository.save(course);
        return toDTO(saved);
    }



    public CourseResponseDTO updateCourse(Long id, CourseRequestDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));


        updateEntityFromDTO(course, dto);
        Course saved = courseRepository.save(course);
        return toDTO(saved);
    }



    public CourseResponseDTO partialUpdateCourse(Long id, CourseRequestDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (dto.getName() != null) {
            course.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null) {
            course.setPrice(dto.getPrice());
        }

        Course saved = courseRepository.save(course);
        return toDTO(saved);
    }



    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Nếu bạn lưu thêm publicId để xoá bên Cloudinary, xử lý tại đây.
        // Ví dụ: cloudinaryService.delete(publicId);

        courseRepository.delete(course);
    }

    // ====== FR-07: UPLOAD IMAGE (Cloudinary) ======

    public CourseResponseDTO uploadImage(Long id, MultipartFile file) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        try {
            // Nếu muốn xoá ảnh cũ trên Cloudinary, cần lưu publicId từ trước rồi gọi delete ở đây.

            String imageUrl = cloudinaryService.uploadFile(file);
            course.setImageUrl(imageUrl);
            Course saved = courseRepository.save(course);
            return toDTO(saved);

        } catch (IllegalArgumentException e) {
            // File rỗng / sai content-type → 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            // Lỗi upload Cloudinary → 500
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload file");
        }
    }

    // ====== FR-08: DELETE IMAGE ======

    public void deleteImage(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getImageUrl() == null) {
            // Theo SRS: nếu không có ảnh → 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course has no image");
        }

        // Nếu có xử lý xoá trên Cloudinary thì gọi thêm service tại đây.

        course.setImageUrl(null);
        courseRepository.save(course);
    }
}

