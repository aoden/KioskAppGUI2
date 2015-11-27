package com.tdt.kioskapp.repository;


import com.tdt.kioskapp.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, String> {
}
