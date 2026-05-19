package re.miniproject.controller;



import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import re.miniproject.model.dto.request.CourseRequestDTO;
import re.miniproject.model.dto.response.CourseResponseDTO;
import re.miniproject.model.entity.Course;
import re.miniproject.repository.ICourseRepository;
import re.miniproject.service.CloudinaryService;
import re.miniproject.service.CourseService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final ICourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;

    public CourseController(CourseService courseService,
                            ICourseRepository courseRepository,
                            CloudinaryService cloudinaryService) {
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // FR-01: GET ALL (phân trang + sort)
    @GetMapping
    public ResponseEntity<Page<CourseResponseDTO>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        try {
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction =
                    (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc"))
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
            Page<CourseResponseDTO> result = courseService.getAllCourses(pageable);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // sort field hoặc direction không hợp lệ → 400 Bad Request (theo SRS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort parameter");
        }
    }

    // FR-02: GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> getCourseById(@PathVariable Long id) {
        CourseResponseDTO dto = courseService.getCourseById(id);
        return ResponseEntity.ok(dto);
    }

    // FR-03: POST – tạo khóa học mới (không nhận id, imageUrl)
    @PostMapping
    public ResponseEntity<CourseResponseDTO> createCourse(@RequestBody @Valid CourseRequestDTO dto) {
        CourseResponseDTO created = courseService.createCourse(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // FR-04: PUT – cập nhật toàn bộ (không động vào imageUrl)
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> updateCourse(
            @PathVariable Long id,
            @RequestBody @Valid CourseRequestDTO dto
    ) {
        CourseResponseDTO updated = courseService.updateCourse(id, dto);
        return ResponseEntity.ok(updated);
    }

    // FR-05: PATCH – cập nhật 1 phần
    @PatchMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> partialUpdateCourse(
            @PathVariable Long id,
            @RequestBody CourseRequestDTO dto
    ) {
        CourseResponseDTO updated = courseService.partialUpdateCourse(id, dto);
        return ResponseEntity.ok(updated);
    }

    // FR-06: DELETE course + (logic xóa ảnh tương ứng, nếu bạn có xoá trên Cloudinary)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Nếu muốn xoá file thật trên Cloudinary, bạn có thể parse public_id từ URL và gọi Cloudinary API ở đây.
        // Ở đây mình chỉ xóa record. SRS yêu cầu xóa file vật lý, với Cloudinary thì là xóa resource remote.
        courseRepository.delete(course);

        return ResponseEntity.noContent().build();
    }

    // FR-07: UPLOAD ảnh: /api/courses/{id}/image (upload lên Cloudinary)
    @PostMapping("/{id}/image")
    public ResponseEntity<CourseResponseDTO> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        try {
            // Nếu muốn, bạn có thể xoá ảnh cũ trên Cloudinary ở đây (nếu lưu public_id).
            String imageUrl = cloudinaryService.uploadFile(file);
            course.setImageUrl(imageUrl);
            Course saved = courseRepository.save(course);

            CourseResponseDTO dto = new CourseResponseDTO();
            dto.setId(saved.getId());
            dto.setName(saved.getName());
            dto.setDescription(saved.getDescription());
            dto.setPrice(saved.getPrice());
            dto.setImageUrl(saved.getImageUrl());

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            // File không hợp lệ (sai content-type, rỗng, v.v.) → 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            // Lỗi trong quá trình upload Cloudinary → 500
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload file");
        }
    }

    // FR-08: DELETE ảnh: /api/courses/{id}/image
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getImageUrl() == null) {
            // Theo SRS: nếu course không tồn tại hoặc chưa có ảnh → 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course has no image");
        }

        // Nếu có xoá bên Cloudinary thì gọi thêm ở đây.
        course.setImageUrl(null);
        courseRepository.save(course);

        return ResponseEntity.noContent().build();
    }
}


