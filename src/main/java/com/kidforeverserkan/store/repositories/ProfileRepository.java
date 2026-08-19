package com.kidforeverserkan.store.repositories;

import com.kidforeverserkan.store.entities.Profile;
import org.springframework.data.repository.CrudRepository;

public interface ProfileRepository extends CrudRepository<Profile, Long> {
}