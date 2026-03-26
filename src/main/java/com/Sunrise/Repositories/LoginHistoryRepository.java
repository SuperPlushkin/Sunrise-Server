package com.Sunrise.Repositories;

import com.Sunrise.Entities.DBs.LoginHistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> { }
