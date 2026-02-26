package com.hybrid9.pg.Lipanasi.repositories.config;



import com.hybrid9.pg.Lipanasi.entities.ApiToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    ApiToken findById(long id);
    ApiToken findByType(String type);

    @Query(value = "SELECT apt.* FROM api_token apt WHERE apt.type LIKE %?1% AND apt.header_key LIKE %?2% AND apt.token LIKE %?3%", countQuery = "SELECT COUNT(*) FROM api_token apt WHERE apt.type LIKE %?1% AND apt.header_key LIKE %?2% AND apt.token LIKE %?3%", nativeQuery = true)
    Page<ApiToken> findAllByTypeAndHeaderKeyAndToken(String type, String headerKey, String token, Pageable pageable);
}
