package com.projet.cnss.repository;
import com.projet.cnss.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String verificationToken);
    // Pour le mot de passe oublié
    Optional<User> findByResetToken(String resetToken);
    boolean existsByEmail(String email);



}