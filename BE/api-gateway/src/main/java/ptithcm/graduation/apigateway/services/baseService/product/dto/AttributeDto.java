package ptithcm.graduation.apigateway.services.baseService.product.dto;

import java.util.List;

public class AttributeDto {
    private Long attributeId;
    private String name;
    private String description;
    private List<AttributeDto> attributeValueList;

    // Default constructor
    public AttributeDto() {}

    // All args constructor
    public AttributeDto(Long attributeId, String name, String description, List<AttributeDto> attributeValueList) {
        this.attributeId = attributeId;
        this.name = name;
        this.description = description;
        this.attributeValueList = attributeValueList;
    }

    // Getters and Setters
    public Long getAttributeId() { return attributeId; }
    public void setAttributeId(Long attributeId) { this.attributeId = attributeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<AttributeDto> getAttributeValueList() { return attributeValueList; }
    public void setAttributeValueList(List<AttributeDto> attributeValueList) { this.attributeValueList = attributeValueList; }
}
