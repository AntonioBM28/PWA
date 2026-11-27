package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.AbonoSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AbonoSaldoRepository extends JpaRepository<AbonoSaldo, UUID> {
}
