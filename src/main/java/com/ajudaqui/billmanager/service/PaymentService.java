package com.ajudaqui.billmanager.service;

import com.ajudaqui.billmanager.service.filter.payments.*;

import static com.ajudaqui.billmanager.utils.StatusBoleto.PAGO;
import static com.ajudaqui.billmanager.utils.ValidarStatus.*;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalDate.of;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.ajudaqui.billmanager.config.serucity.JwtUtils;
import com.ajudaqui.billmanager.controller.from.BoletoFrom;
import com.ajudaqui.billmanager.entity.Category;
import com.ajudaqui.billmanager.entity.Payment;
import com.ajudaqui.billmanager.entity.Users;
import com.ajudaqui.billmanager.exception.MsgException;
import com.ajudaqui.billmanager.repository.PaymentsRepository;
import com.ajudaqui.billmanager.service.vo.PayamentDto;
import com.ajudaqui.billmanager.service.vo.Sumary;
import com.ajudaqui.billmanager.utils.StatusBoleto;
import com.ajudaqui.billmanager.utils.ValidarStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javassist.NotFoundException;

@Service
public class PaymentService {

  private static final String ATT_PAYMENT = "att-payment";
  private JwtUtils jwtUtils;
  private PaymentsRepository paymentRepository;
  private UsersService usersService;
  private CategoryService categoryService;

  public PaymentService(PaymentsRepository paymentRepository, UsersService usersService,
      CategoryService categoryService, JwtUtils jwtUtils) {
    this.paymentRepository = paymentRepository;
    this.usersService = usersService;
    this.categoryService = categoryService;
    this.jwtUtils = jwtUtils;
  }

  private Logger logger = LoggerFactory.getLogger(PaymentService.class.getSimpleName());

  public Payment register(PayamentDto paymentDto, String accessToken) {
    Users users = usersService.findByAccessToken(accessToken);
    Payment payment = paymentDto.toDatabase(users);

    if (isRegistery(payment))
      throw new MsgException("pagamento já cadastrado");
    return save(payment);
  }

  private Payment save(Payment payment) {
    if (payment.getUser().isCalControl())
      sendToKafka(payloadPayments(payment.getUser().getAccessToken(), payment.getDueDate()), ATT_PAYMENT);

    return paymentRepository.save(payment);
  }

  public Payment update(Payment payment) {
    return save(payment);
  }

  private List<Payment> buscarNovasOcorrenciasDoBoletoId(String accessToken, Long paymentId) {

    List<Payment> payment = paymentRepository.buscarNovasOcorrenciasDoBoletoId(paymentId);

    if (payment.isEmpty())
      throw new MsgException("Nenhum boleto localizado");

    if (!accessToken.equals(payment.get(0).getUser().getAccessToken()))
      throw new MsgException("Solicitaã́o não autorizada");
    return payment;

  }

  public List<Payment> boletosRecorrentes(PayamentDto paymentDto, Long repeticao, String accessToken) {
    List<Payment> registeredPayments = new ArrayList<>();

    int index = 0;
    Users users = usersService.findByAccessToken(accessToken);
    Category category = null;
    if (paymentDto.getCategory() != null)
      category = categoryService.findByNameOrRegister(paymentDto.getCategory(), users);

    while (index < repeticao) {
      Payment newPayment = paymentDto.toDatabase(users);
      if (category != null)
        newPayment.setCategory(category);
      newPayment.setDueDate(newPayment.getDueDate().plusMonths(index));
      index++;
      if (isRegistery(newPayment)) {
        logger.warn("Boleto descrição: {}, valor: {}, Vencimento: {} já registrado.",
            newPayment.getDescription(),
            newPayment.getValue(),
            newPayment.getDueDate());
        continue;
      }
      registeredPayments.add(save(newPayment));
    }
    return registeredPayments;
  }

  protected boolean isRegistery(Payment payment) {
    List<Payment> paymentForMonth = findAllMonth(payment.getUser().getAccessToken(),
        payment.getDueDate().getMonthValue(),
        payment.getDueDate().getYear());
    if (paymentForMonth.isEmpty())
      return false;
    return paymentForMonth.contains(payment);
  }

  public Payment findByIdForUsers(String accessToken, Long paymentId) {
    return paymentRepository.findByIdForUsers(jwtUtils.getAccessTokenFromJwt(accessToken), paymentId)
        .orElseThrow(() -> new MsgException("Boleto não encontrado."));
  }

  public Payment findById(Long paymentId) {
    return paymentRepository.findById(paymentId)
        .orElseThrow(() -> new MsgException("Boleto não encontrado."));
  }

  public List<Payment> findByPayamentsForUser(String accessToken) {
    return paymentRepository.findByPaymentsForUserAccessToken(jwtUtils.getAccessTokenFromJwt(accessToken));
  }

  public List<Payment> findPaymentsWeek(String accessToken, String date, String status) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate dayWeek = LocalDate.parse(date, formatter);
    LocalDate monday = dayWeek.minusDays(dayWeek.minusDays(1).getDayOfWeek().getValue());
    LocalDate sunday = monday.plusDays(6);

    accessToken = jwtUtils.getAccessTokenFromJwt(accessToken);
    if (status.isEmpty())
      return paymentRepository.findPayaments(accessToken, monday, sunday);
    return paymentRepository.findPayaments(accessToken, monday, sunday,
        StatusBoleto.valueOf(status));
  }

  public List<Payment> findAllMonth(String accessToken, Integer month, Integer year) {
    LocalDate startMonth = of(year, month, 1);
    LocalDate endMonth = of(year, month, startMonth.lengthOfMonth());
    return paymentRepository.findPayaments(jwtUtils.getAccessTokenFromJwt(accessToken), startMonth, endMonth);
  }

  public Payment update(String accessToken, Long paymentId, BoletoFrom from) {

    List<Payment> payments = buscarNovasOcorrenciasDoBoletoId(accessToken, paymentId);
    Category category = null;
    if (from.getCategory() != null && !from.getCategory().isEmpty()) {
      category = categoryService.findByNameOrRegister(from.getCategory(), payments.get(0).getUser());
    }

    for (Payment payment : payments) {
      payment = from.toUpdate(payment, category);
      save(statusAtualizado(payment));
    }

    return payments.get(0);
  }

  private void sendToKafka(Map<String, Object> payloadPayments, String topic) {
    // kafkaProducer.sendMessage(topic, payloadPayments);
  }

  // atualização do estado em execução
  public void performStatusUpdate() {
    List<Payment> pagamentos = paymentRepository.nextPayments(LocalDate.now().plusDays(10));
    atualizarStatus(pagamentos);
  }

  public List<Payment> atualizarStatus(List<Payment> boletos) {
    boletos.forEach(ValidarStatus::statusAtualizado);
    return boletos;
  }

  public void deleteById(String accessToken, Long id) {
    Payment payment = findById(id);
    if (!accessToken.equals(payment.getUser().getAccessToken()))
      throw new MsgException("Solicitação não autorizada");

    paymentRepository.delete(payment);
    if (payment.getUser().isCalControl())
      sendToKafka(payloadPayments(payment.getUser().getAccessToken(), payment.getDueDate()), ATT_PAYMENT);

  }

  public List<Payment> periodTime(String accessToken, String description, LocalDate start, LocalDate finish,
      String status) {
    Users user = usersService.findByAccessToken(accessToken);
    start = (start == null) ? LocalDate.now() : start;
    finish = (finish == null) ? LocalDate.now() : finish;

    return FilterSelection.searchFilter(paymentRepository, user.getAccessToken(), description, start, finish,
        status);
  }

  public Sumary sumary(String accessToken, LocalDate start, LocalDate finsh) {
    List<Payment> payments = periodTime(accessToken, "", start, finsh, "");
    BigDecimal totalDue = payments.stream()
        .map(Payment::getValue)
        .reduce(ZERO, BigDecimal::add);

    BigDecimal amountPaid = payments.stream()
        .filter(payment -> payment.getStatus().equals(PAGO))
        .map(Payment::getValue)
        .reduce(ZERO, BigDecimal::add);
    return new Sumary(totalDue, amountPaid);
  }

  public Payment confirmPayment(String accessToken, Long paymentId) {
    Payment payment = findByIdForUsers(accessToken, paymentId);
    payment.setStatus(PAGO);
    payment.setUpdatedAt(now());
    return paymentRepository.save(payment);
  }

  public Map<String, BigDecimal> sumaryCategory(String accessToken, LocalDate start, LocalDate finsh) {
    List<Payment> payments = periodTime(accessToken, "", start, finsh, "");
    Map<String, BigDecimal> sumary = payments.stream()
        .collect(groupingBy(
            p -> nameCategoryFactor(p.getCategory()),
            mapping(Payment::getValue, Collectors.reducing(ZERO, BigDecimal::add))));
    sumary.replaceAll((k, v) -> v.setScale(2, HALF_UP));
    return sumary;
  }

  private String nameCategoryFactor(Category category) {
    return category != null ? category.getName() : "sem-categoria";
  }

  public Payment addCategory(String accessToken, Long paymentId, String name) {
    Users users = usersService.findByAccessToken(accessToken);

    Category category = categoryService.findByNameOrRegister(name, users);
    Payment payment = findById(paymentId);
    category.getPayments().add(payment);
    payment.setCategory(category);

    categoryService.update(category);
    return payment;
  }

  private Map<String, Object> payloadPayments(String accessToken, LocalDate start) {
    List<Payment> payments = periodTime(accessToken, "", start, start, "");
    Map<String, Object> payload = new HashMap<>();
    payload.put("accessToken", accessToken);
    payload.put("day", start);
    BigDecimal total = payments.stream()
        .map(Payment::getValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    payload.put("total", total);
    List<PayamentDto> paymentsDto = payments.stream().map(PayamentDto::new).collect(Collectors.toList());
    payload.put("payments", paymentsDto);
    return payload;

  }
}
