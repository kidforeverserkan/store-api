package com.kidforeverserkan.store.products;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    ProductDto toDto(Product product);

    // id is server-assigned: copying a client-supplied id here made
    // POST /products overwrite the existing product with that id.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    Product toEntity(ProductDto productDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void update(
            ProductDto productDto,
            @MappingTarget Product product
    );
}