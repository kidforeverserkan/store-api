package com.kidforeverserkan.store.repositories;

import com.kidforeverserkan.store.products.Category;
import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Byte> {
}