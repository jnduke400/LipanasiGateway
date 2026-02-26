package com.hybrid9.pg.Lipanasi.repositories.operators;


import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MnoPrefixRepository extends JpaRepository<MnoPrefix, Long> {
    MnoPrefix findByPrefix(String prefix);
    MnoPrefix findTop1ByMnoMapping(MnoMapping mnoMapping);

    List<MnoPrefix> findAllByMnoMapping(MnoMapping mnoMapping);
    @Query(value = "SELECT * FROM tbl_sms_mno_prefixies AS prfx WHERE (prfx.dial_code like %:searchTerm% or prfx.country_name like %:searchTerm%  or prfx.prefix like %:searchTerm%)", nativeQuery = true)
    Page<MnoPrefix> findAllPrefix(@Param("searchTerm") String searchTerm, Pageable pageable);
}