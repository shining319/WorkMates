package com.workmates.account.repository;
import com.workmates.account.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface UserRepository extends JpaRepository<User,Long>{
 @Query("select u from User u where lower(u.email)=:email") Optional<User> findByEmailIgnoreCase(@Param("email") String normalized);
 @Query("select count(u)>0 from User u where lower(u.email)=:email") boolean existsByEmailIgnoreCase(@Param("email") String normalized);
}
