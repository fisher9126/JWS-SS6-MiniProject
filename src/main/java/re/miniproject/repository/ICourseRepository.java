package re.miniproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import re.miniproject.model.entity.Course;

public interface ICourseRepository extends JpaRepository<Course,Long> {
}
