package com.app.uctma.backend.repository;

import com.app.uctma.backend.model.AuthAccountSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthAccountSettingRepository extends JpaRepository<AuthAccountSetting, Long> {
    List<AuthAccountSetting> findAllByOrderByIdAsc();
    Optional<AuthAccountSetting> findBySettingCode(String settingCode);
}
