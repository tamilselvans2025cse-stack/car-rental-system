package com.example.carrental.repository;

import com.example.carrental.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {
    List<Resource> findByStatus(String status);
    List<Resource> findByCategoryIgnoreCase(String category);
    List<Resource> findByNameContainingIgnoreCase(String name);
    List<Resource> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);
}
