package com.hybrid9.pg.Lipanasi.repositories.config;



import com.hybrid9.pg.Lipanasi.entities.Api;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRepository extends JpaRepository<Api, Long> {

    Api findById(long id);
    Api findByTypeAndHeaderKey(String type, String headerKey);

    Page<Api> findAllByType(String type, Pageable pageable);

    @Query(value = "SELECT a.* FROM api a WHERE a.type LIKE %?1% AND a.url LIKE %?2% AND a.header_key LIKE %?3% AND a.header_value LIKE %?4%", countQuery = "SELECT COUNT(*) FROM api a WHERE a.type LIKE %?1% AND a.url LIKE %?2% AND a.header_key LIKE %?3% AND a.header_value LIKE %?4%", nativeQuery = true)
    Page<Api> findAllByTypeAndUrlAndHeaderKeyAndHeaderValue(String type, String url, String headerKey, String headerValue, Pageable pageable);
}
