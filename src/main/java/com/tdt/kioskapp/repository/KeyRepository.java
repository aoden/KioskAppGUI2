package com.tdt.kioskapp.repository;

import com.tdt.kioskapp.model.Key;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyRepository extends JpaRepository<Key, String> {
}
