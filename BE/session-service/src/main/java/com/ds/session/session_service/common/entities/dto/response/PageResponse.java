package com.ds.session.session_service.common.entities.dto.response;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Page;

/**
 * DTO chung cho phản hồi (response) dạng phân trang (pagination).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    /**
     * Phương thức tiện ích để chuyển đổi từ đối tượng Page của Spring Data.
     * @param page Đối tượng Page<T> từ repository
     * @return PageResponse<T>
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

     /**
     * Phương thức tiện ích để chuyển đổi từ đối tượng Page của Spring Data,
     * nhưng giữ lại nội dung của kiểu DTO (nếu đã được map).
     * @param page Đối tượng Page<?> từ repository
     * @param content Danh sách DTO đã được map
     * @return PageResponse<T>
     */
    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
