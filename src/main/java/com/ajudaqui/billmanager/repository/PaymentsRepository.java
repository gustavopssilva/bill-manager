package com.ajudaqui.billmanager.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ajudaqui.billmanager.entity.Payment;
import com.ajudaqui.billmanager.utils.StatusBoleto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentsRepository extends JpaRepository<Payment, Long> {

  @Query("SELECT b FROM Payment b WHERE b.user.accessToken = :accessToken AND b.dueDate >= :inicioMes AND b.dueDate <= :finalMes")
  List<Payment> findPayaments(String accessToken, LocalDate inicioMes, LocalDate finalMes);

  @Query("SELECT b FROM Payment b WHERE b.user.accessToken = :accessToken AND b.description = :description AND b.dueDate >= :inicioMes AND b.dueDate <= :finalMes")
  List<Payment> findPayaments(String accessToken, String description, LocalDate inicioMes, LocalDate finalMes);

  @Query("SELECT b FROM Payment b WHERE b.user.accessToken = :accessToken AND b.status = :status AND b.dueDate >= :inicioMes AND b.dueDate <= :finalMes")
  List<Payment> findPayaments(String accessToken, LocalDate inicioMes, LocalDate finalMes, StatusBoleto status);

  @Query("SELECT b FROM Payment b WHERE b.user.accessToken = :accessToken AND b.description = :description AND b.status = :status AND b.dueDate >= :inicioMes AND b.dueDate <= :finalMes")
  List<Payment> findPayaments(String accessToken, String description, LocalDate inicioMes, LocalDate finalMes,
      StatusBoleto status);

  @Query(value = "select * from payment where status <> 'PAGO' AND dueDate > :deadline ", nativeQuery = true)
  List<Payment> nextPayments(LocalDate deadline);

  @Query(value = "select * from payment where users_id = (select id from users where accessToken = :accessToken)", nativeQuery = true)
  List<Payment> findByPaymentsForUserAccessToken(String accessToken);

  @Query("SELECT b FROM Payment b WHERE b.user.accessToken = :accessToken AND b.id = :paymentId")
  Optional<Payment> findByIdForUsers(String accessToken, Long paymentId);

  @Query(value = "SELECT p2.* " +
      "FROM payment p2 " +
      "CROSS JOIN ( " +
      "    SELECT description, " +
      "           value, " +
      "           date_trunc('month', due_date) AS due_month " +
      "    FROM payment " +
      "    WHERE id = :paymentId " +
      ") p1 " +
      "WHERE p2.description = p1.description " +
      "  AND p2.value = p1.value " +
      "  AND p2.due_date >= p1.due_month " +
      "ORDER BY p2.due_date", nativeQuery = true)
  List<Payment> buscarNovasOcorrenciasDoBoletoId(Long paymentId);

}
