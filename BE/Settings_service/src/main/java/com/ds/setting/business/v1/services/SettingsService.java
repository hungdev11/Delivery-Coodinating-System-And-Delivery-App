package com.ds.setting.business.v1.services;

import com.ds.setting.app_context.models.SystemSetting;
import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import com.ds.setting.app_context.repositories.SystemSettingRepository;
import com.ds.setting.common.entities.dto.CreateSettingRequest;
import com.ds.setting.common.entities.dto.SystemSettingDto;
import com.ds.setting.common.entities.dto.UpdateSettingRequest;
import com.ds.setting.common.entities.dto.common.PagedData;
import com.ds.setting.common.entities.dto.common.PagingRequest;
import com.ds.setting.common.entities.dto.common.FilterGroup;
import com.ds.setting.common.entities.dto.common.FilterCondition;
import com.ds.setting.common.entities.dto.common.SortConfig;
import com.ds.setting.common.exceptions.SettingNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for managing system settings
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SystemSettingRepository settingRepository;

    public PagedData<SystemSettingDto> getSettings(PagingRequest query) {
        int page = query.getPageOrDefault();
        int size = query.getSizeOrDefault();

        Specification<SystemSetting> spec = buildSpecification(query.getFiltersOrEmpty());
        Sort sort = buildSort(query.getSortsOrEmpty());

        var pageable = PageRequest.of(page, size, sort);
        var result = settingRepository.findAll(spec, pageable);

        List<SystemSettingDto> data = result.getContent().stream().map(this::toDto).toList();

        PagedData.Paging paging = PagedData.Paging.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .filters(query.getFiltersOrEmpty())
                .sorts(query.getSortsOrEmpty())
                .selected(query.getSelectedOrEmpty())
                .build();

        return PagedData.<SystemSettingDto>builder()
                .data(data)
                .page(paging)
                .build();
    }

    private Specification<SystemSetting> buildSpecification(FilterGroup filterGroup) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Handle null conditions
            List<Object> conditions = filterGroup.getConditions();
            if (conditions == null || conditions.isEmpty()) {
                return null;
            }
            
            for (Object node : conditions) {
                if (node instanceof FilterGroup nested) {
                    Predicate p = buildSpecification(nested).toPredicate(root, cq, cb);
                    if (p != null) predicates.add(p);
                } else if (node instanceof FilterCondition condition) {
                    Predicate p = buildPredicate(cb, root.get(condition.getField()), condition);
                    if (p != null) predicates.add(p);
                }
            }
            if (predicates.isEmpty()) return null;
            if (Objects.equals(filterGroup.getLogic(), "OR")) {
                return cb.or(predicates.toArray(new Predicate[0]));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildPredicate(jakarta.persistence.criteria.CriteriaBuilder cb,
                                     jakarta.persistence.criteria.Path<?> path,
                                     FilterCondition condition) {
        Object value = condition.getValue();
        switch (condition.getOperator()) {
            case "eq":
                return cb.equal(path, value);
            case "ne":
                return cb.notEqual(path, value);
            case "gt":
                return cb.greaterThan((jakarta.persistence.criteria.Path<Comparable>) path, (Comparable) value);
            case "gte":
                return cb.greaterThanOrEqualTo((jakarta.persistence.criteria.Path<Comparable>) path, (Comparable) value);
            case "lt":
                return cb.lessThan((jakarta.persistence.criteria.Path<Comparable>) path, (Comparable) value);
            case "lte":
                return cb.lessThanOrEqualTo((jakarta.persistence.criteria.Path<Comparable>) path, (Comparable) value);
            case "between":
                if (value instanceof List<?> list && list.size() >= 2) {
                    return cb.between((jakarta.persistence.criteria.Path<Comparable>) path,
                            (Comparable) list.get(0), (Comparable) list.get(1));
                }
                return null;
            case "contains":
                return cb.like(cb.lower((jakarta.persistence.criteria.Expression<String>) path), "%" + String.valueOf(value).toLowerCase() + "%");
            case "startsWith":
                return cb.like(cb.lower((jakarta.persistence.criteria.Expression<String>) path), String.valueOf(value).toLowerCase() + "%");
            case "endsWith":
                return cb.like(cb.lower((jakarta.persistence.criteria.Expression<String>) path), "%" + String.valueOf(value).toLowerCase());
            case "isNull":
                return cb.isNull(path);
            case "isNotNull":
                return cb.isNotNull(path);
            default:
                return null;
        }
    }

    private Sort buildSort(List<SortConfig> sorts) {
        if (sorts == null || sorts.isEmpty()) return Sort.unsorted();
        List<Sort.Order> orders = new ArrayList<>();
        for (SortConfig s : sorts) {
            Sort.Direction dir = "desc".equalsIgnoreCase(s.getDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(dir, s.getField()));
        }
        return Sort.by(orders);
    }

    /**
     * Get setting by key
     */
    @Cacheable(value = "settings", key = "#key")
    public SystemSettingDto getByKey(String key) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new SettingNotFoundException("Setting not found: " + key));
        return toDto(setting);
    }

    /**
     * Get setting value by key (convenient method)
     */
    @Cacheable(value = "settings", key = "'value_' + #key")
    public String getValue(String key) {
        return getByKey(key).getValue();
    }

    /**
     * Get setting value with default
     */
    public String getValue(String key, String defaultValue) {
        try {
            return getValue(key);
        } catch (SettingNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Get setting by key and group (composite lookup)
     */
    @Cacheable(value = "settings", key = "#key + '_' + #group")
    public SystemSettingDto getByKeyAndGroup(String key, String group) {
        SystemSetting setting = settingRepository.findByKeyAndGroup(key, group)
                .orElseThrow(() -> new SettingNotFoundException(
                        String.format("Setting not found: key=%s, group=%s", key, group)));
        return toDto(setting);
    }

    /**
     * Get setting value by key and group
     */
    @Cacheable(value = "settings", key = "'value_' + #key + '_' + #group")
    public String getValueByKeyAndGroup(String key, String group) {
        return getByKeyAndGroup(key, group).getValue();
    }

    /**
     * Get setting value by key and group with default
     */
    public String getValueByKeyAndGroup(String key, String group, String defaultValue) {
        try {
            return getValueByKeyAndGroup(key, group);
        } catch (SettingNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Get all settings by group
     */
    @Cacheable(value = "settingsByGroup", key = "#group")
    public List<SystemSettingDto> getByGroup(String group) {
        return settingRepository.findByGroup(group).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all settings by level
     */
    public List<SystemSettingDto> getByLevel(SettingLevel level) {
        return settingRepository.findByLevel(level).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all settings
     */
    public List<SystemSettingDto> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Search settings
     */
    public List<SystemSettingDto> searchSettings(String search) {
        return settingRepository.searchSettings(search).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new setting
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public SystemSettingDto createSetting(CreateSettingRequest request) {
        // Check if setting already exists
        if (settingRepository.existsById(request.getKey())) {
            throw new IllegalArgumentException("Setting already exists: " + request.getKey());
        }

        SystemSetting setting = SystemSetting.builder()
                .key(request.getKey())
                .group(request.getGroup())
                .description(request.getDescription())
                .type(request.getType())
                .value(request.getValue())
                .level(request.getLevel())
                .isReadOnly(request.getIsReadOnly())
                .displayMode(request.getDisplayMode())
                .build();

        SystemSetting saved = settingRepository.save(setting);
        log.info("Created setting: key={}, group={}, level={}", saved.getKey(), saved.getGroup(), saved.getLevel());

        return toDto(saved);
    }

    /**
     * Upsert (Create or Update) a setting by key and group
     * This is the main API for settings modification
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public SystemSettingDto upsertByKeyAndGroup(String key, String group, CreateSettingRequest request, String userId) {
        return settingRepository.findByKeyAndGroup(key, group)
                .map(existing -> {
                    // Update existing setting
                    if (request.getDescription() != null) {
                        existing.setDescription(request.getDescription());
                    }
                    if (request.getType() != null) {
                        existing.setType(request.getType());
                    }
                    if (request.getValue() != null) {
                        existing.setValue(request.getValue());
                    }
                    if (request.getLevel() != null) {
                        existing.setLevel(request.getLevel());
                    }
                    if (request.getDisplayMode() != null) {
                        existing.setDisplayMode(request.getDisplayMode());
                    }
                    existing.setUpdatedBy(userId);
                    
                    SystemSetting updated = settingRepository.save(existing);
                    log.info("Updated setting: key={}, group={}, updatedBy={}", key, group, userId);
                    return toDto(updated);
                })
                .orElseGet(() -> {
                    // Create new setting
                    SystemSetting setting = SystemSetting.builder()
                            .key(key)
                            .group(group)
                            .description(request.getDescription())
                            .type(request.getType())
                            .value(request.getValue())
                            .level(request.getLevel())
                            .isReadOnly(request.getIsReadOnly() != null ? request.getIsReadOnly() : false)
                            .displayMode(request.getDisplayMode() != null ? request.getDisplayMode() : SystemSetting.DisplayMode.TEXT)
                            .updatedBy(userId)
                            .build();
                    SystemSetting saved = settingRepository.save(setting);
                    log.info("Created setting: key={}, group={}, createdBy={}", key, group, userId);
                    return toDto(saved);
                });
    }

    /**
     * Update a setting
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public SystemSettingDto updateSetting(String key, UpdateSettingRequest request, String updatedBy) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new SettingNotFoundException("Setting not found: " + key));

        // Update fields if provided
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }

        if (request.getType() != null) {
            setting.setType(request.getType());
        }

        if (request.getValue() != null) {
            setting.setValue(request.getValue());
        }

        if (request.getDisplayMode() != null) {
            setting.setDisplayMode(request.getDisplayMode());
        }

        setting.setUpdatedBy(updatedBy);

        SystemSetting updated = settingRepository.save(setting);
        log.info("Updated setting: key={}, updatedBy={}", key, updatedBy);

        return toDto(updated);
    }

    /**
     * Update a setting by key and group
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public SystemSettingDto updateByKeyAndGroup(String key, String group, UpdateSettingRequest request, String updatedBy) {
        SystemSetting setting = settingRepository.findByKeyAndGroup(key, group)
                .orElseThrow(() -> new SettingNotFoundException(
                        String.format("Setting not found: key=%s, group=%s", key, group)));

        // Update fields if provided
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }

        if (request.getType() != null) {
            setting.setType(request.getType());
        }

        if (request.getValue() != null) {
            setting.setValue(request.getValue());
        }

        if (request.getDisplayMode() != null) {
            setting.setDisplayMode(request.getDisplayMode());
        }

        setting.setUpdatedBy(updatedBy);

        SystemSetting updated = settingRepository.save(setting);
        log.info("Updated setting: key={}, group={}, updatedBy={}", key, group, updatedBy);

        return toDto(updated);
    }

    /**
     * Delete a setting by key and group
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public void deleteByKeyAndGroup(String key, String group) {
        SystemSetting setting = settingRepository.findByKeyAndGroup(key, group)
                .orElseThrow(() -> new SettingNotFoundException(
                        String.format("Setting not found: key=%s, group=%s", key, group)));

        settingRepository.delete(setting);
        log.info("Deleted setting: key={}, group={}", key, group);
    }

    /**
     * Convert entity to DTO
     */
    private SystemSettingDto toDto(SystemSetting setting) {
        return SystemSettingDto.builder()
                .key(setting.getKey())
                .group(setting.getGroup())
                .description(setting.getDescription())
                .type(setting.getType())
                .value(setting.getValue())
                .level(setting.getLevel())
                .isReadOnly(setting.getIsReadOnly())
                .displayMode(setting.getDisplayMode())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .updatedBy(setting.getUpdatedBy())
                .build();
    }

    public PagedData<SystemSettingDto> getSettingsV0(com.ds.setting.common.entities.dto.request.PagingRequestV0 query) {
        // V0: Simple paging with sorting only, no dynamic filters
        int page = query.getPage();
        int size = query.getSize();

        Sort sort = query.getSortsOrEmpty() != null && !query.getSortsOrEmpty().isEmpty()
            ? buildSortFromConfigs(query.getSortsOrEmpty())
            : Sort.by(Sort.Direction.DESC, "key");

        var pageable = PageRequest.of(page, size, sort);
        var result = settingRepository.findAll(pageable);

        List<SystemSettingDto> data = result.getContent().stream().map(this::toDto).toList();

        PagedData.Paging paging = PagedData.Paging.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .filters(null)
                .sorts(query.getSortsOrEmpty())
                .selected(query.getSelectedOrEmpty())
                .build();

        return PagedData.<SystemSettingDto>builder()
                .data(data)
                .page(paging)
                .build();
    }

    public PagedData<SystemSettingDto> getSettingsV2(com.ds.setting.common.entities.dto.request.PagingRequestV2 query) {
        // V2: Enhanced filtering with operations between each pair
        int page = query.getPage();
        int size = query.getSize();

        Specification<SystemSetting> spec = Specification.where(null);
        if (query.getFiltersOrNull() != null) {
            spec = com.ds.setting.common.utils.EnhancedQueryParserV2.parseFilterGroup(
                query.getFiltersOrNull(),
                SystemSetting.class
            );
        }

        Sort sort = query.getSortsOrEmpty() != null && !query.getSortsOrEmpty().isEmpty()
            ? buildSortFromConfigs(query.getSortsOrEmpty())
            : Sort.by(Sort.Direction.DESC, "key");

        var pageable = PageRequest.of(page, size, sort);
        var result = settingRepository.findAll(spec, pageable);

        List<SystemSettingDto> data = result.getContent().stream().map(this::toDto).toList();

        PagedData.Paging paging = PagedData.Paging.builder()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .filters(null)
                .sorts(query.getSortsOrEmpty())
                .selected(query.getSelectedOrEmpty())
                .build();

        return PagedData.<SystemSettingDto>builder()
                .data(data)
                .page(paging)
                .build();
    }

    private Sort buildSortFromConfigs(List<com.ds.setting.common.entities.dto.common.SortConfig> sortConfigs) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "key");
        }

        List<Sort.Order> orders = sortConfigs.stream()
            .map(s -> "asc".equalsIgnoreCase(s.getDirection()) 
                ? Sort.Order.asc(s.getField()) 
                : Sort.Order.desc(s.getField()))
            .toList();

        return Sort.by(orders);
    }
}
