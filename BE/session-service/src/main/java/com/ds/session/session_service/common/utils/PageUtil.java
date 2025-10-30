package com.ds.session.session_service.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public class PageUtil {

    /**
     * Xây dựng đối tượng Pageable với sắp xếp.
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @param sortBy Tên trường để sắp xếp (ví dụ: "scanedAt")
     * @param direction "asc" (tăng) hoặc "desc" (giảm)
     * @param entityClass Class của Entity (để kiểm tra tên trường, tránh SQL Injection)
     * @return Pageable
     */
    public static Pageable build(int page, int size, String sortBy, String direction, Class<?> entityClass) {
        // Đảm bảo giá trị tối thiểu và tối đa
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size)); // Giới hạn size tối đa 100

        Sort sort = Sort.unsorted();
        if (StringUtils.hasText(sortBy) && fieldExists(entityClass, sortBy)) {
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(sortDirection, sortBy);
        }

        return PageRequest.of(safePage, safeSize, sort);
    }

    /**
     * Kiểm tra xem tên trường có tồn tại trong Entity không
     */
    private static boolean fieldExists(Class<?> entityClass, String fieldName) {
        if (fieldName == null || entityClass == null) {
            return false;
        }
        try {
            // Kiểm tra trường (field)
            entityClass.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            // Kiểm tra trường của lớp cha (nếu có)
            try {
                 if (entityClass.getSuperclass() != null) {
                    entityClass.getSuperclass().getDeclaredField(fieldName);
                    return true;
                 }
            } catch (NoSuchFieldException ex) {
                // Không tìm thấy
            }
            return false;
        }
    }
}
