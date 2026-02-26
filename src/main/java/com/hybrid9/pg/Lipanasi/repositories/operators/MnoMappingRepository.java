package com.hybrid9.pg.Lipanasi.repositories.operators;



import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository
public interface MnoMappingRepository extends JpaRepository<MnoMapping, Long> {
    @Query(value = "SELECT * FROM tbl_mno_mapping mn WHERE mn.mno_prefix like %:prefix%",nativeQuery = true)
    MnoMapping searchMno(@Param("prefix") String prefix);
    @Query(value = "SELECT * FROM tbl_mno_mapping AS mn WHERE (mn.mno like %:searchTerm% or mn.ip like %:searchTerm% or mn.port like %:searchTerm% or mn.username like %:searchTerm% or mn.prefix like %:searchTerm% or mn.route_to like %:searchTerm%)",nativeQuery = true)
    Page<MnoMapping> findAll(@Param("searchTerm") String searchTerm, Pageable pageable);

    MnoMapping findByMno(String operator);
}