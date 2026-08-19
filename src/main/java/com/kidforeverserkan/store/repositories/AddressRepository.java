package com.kidforeverserkan.store.repositories;

import com.kidforeverserkan.store.entities.Address;
import org.springframework.data.repository.CrudRepository;

public interface AddressRepository extends CrudRepository<Address, Long> {
}