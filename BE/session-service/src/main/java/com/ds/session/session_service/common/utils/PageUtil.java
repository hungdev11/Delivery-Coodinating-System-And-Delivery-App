package com.ds.session.session_service.common.utils;


import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageUtil {

    public static Pageable build(int page, int size, String sortBy, String direction, Class<?> entityClass) {
        Sort sort = Sort.unsorted();

        if (isValidSortFieldDeep(entityClass, direction)) {
            sort = direction != null && direction.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
        }

        return PageRequest.of(
                Math.max(page, 0),               
                Math.max(size, 1),            
                sort
        );
    }

    public static <T> Page<T> toPage(List<T> list, Pageable pageable) {
        if (list == null || list.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<T> pagedList;

        if (list.size() < startItem) {
            pagedList = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, list.size());
            pagedList = list.subList(startItem, toIndex);
        }

        return new PageImpl<>(pagedList, pageable, list.size());
    }

    public static boolean isValidSortFieldDeep(Class<?> entityClass, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) return false;
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getName().equals(fieldName)) return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
