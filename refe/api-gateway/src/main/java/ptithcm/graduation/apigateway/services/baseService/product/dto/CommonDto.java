package ptithcm.graduation.apigateway.services.baseService.product.dto;

public class CommonDto {
    
    public static class PageRequestDto {
        private Integer page;
        private Integer size;

        public PageRequestDto() {}

        public PageRequestDto(Integer page, Integer size) {
            this.page = page;
            this.size = size;
        }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    public static class PageInfoDto {
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private Long totalElements;

        public PageInfoDto() {}

        public PageInfoDto(Integer page, Integer size, Integer totalPages, Long totalElements) {
            this.page = page;
            this.size = size;
            this.totalPages = totalPages;
            this.totalElements = totalElements;
        }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public Long getTotalElements() { return totalElements; }
        public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }
    }

    public static class UUIDValueDto {
        private String id;

        public UUIDValueDto() {}

        public UUIDValueDto(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
