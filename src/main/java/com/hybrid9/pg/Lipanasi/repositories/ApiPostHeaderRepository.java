package com.hybrid9.pg.Lipanasi.repositories;

import com.hybrid9.pg.Lipanasi.entities.ApiPostHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ApiPostHeaderRepository extends JpaRepository<ApiPostHeader, Long> {
}